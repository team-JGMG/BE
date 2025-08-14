package org.bobj.payment.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.common.constants.ErrorCode;
import org.bobj.payment.client.PortOneClient;
import org.bobj.payment.domain.PaymentEventType;
import org.bobj.payment.domain.PaymentEventVO;
import org.bobj.payment.domain.PaymentStatus;
import org.bobj.payment.domain.PaymentVO;
import org.bobj.payment.dto.PortOnePaymentResponse;
import org.bobj.payment.dto.PortOnePaymentResponse.PaymentData;
import org.bobj.payment.dto.VerifyRequestDto;
import org.bobj.payment.dto.WebhookDto;
import org.bobj.payment.exception.PaymentException;
import org.bobj.payment.repository.PaymentEventRepository;
import org.bobj.payment.repository.PaymentRepository;
import org.bobj.point.domain.PointChargeRequestVO;
import org.bobj.point.service.PointChargeRequestService;
import org.bobj.point.service.PointService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PortOneClient portOneClient;
    private final PaymentRepository paymentRepository;
    private final PointService pointService;
    private final PointChargeRequestService pointChargeRequestService;
    private final PaymentEventRepository paymentEventRepository;

    public enum EventSource {VERIFY, WEBHOOK}

    @Transactional
    public void verifyWithPortOneAndApply(WebhookDto webhookDto, EventSource source) {
        final String impUid = webhookDto.getImpUid();

        PortOnePaymentResponse res = portOneClient.getPaymentInfo(impUid);
        PaymentData pg = res.getResponse();
        if (pg == null) {
            throw new PaymentException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        PointChargeRequestVO chargeReq =
            pointChargeRequestService.findByMerchantUid(pg.getMerchantUid());
        if (chargeReq == null) {
            throw new PaymentException(ErrorCode.PAYMENT_REQUEST_NOT_FOUND);
        }

        final Long userId = chargeReq.getUserId();
        applyEventFromPaymentData(userId, chargeReq, pg, source);
    }

    @Transactional
    public void verifyPayment(Long userId, VerifyRequestDto requestDto) {
        final String impUid = requestDto.getImpUid();

        PortOnePaymentResponse response = portOneClient.getPaymentInfo(impUid);
        PaymentData pg = response.getResponse();
        if (pg == null) {
            throw new PaymentException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        PointChargeRequestVO chargeReq =
            pointChargeRequestService.findByMerchantUid(pg.getMerchantUid());
        if (chargeReq == null) {
            throw new PaymentException(ErrorCode.PAYMENT_REQUEST_NOT_FOUND);
        }

        if (userId == null) userId = chargeReq.getUserId();


        if (requestDto.getAmount() != null &&
            requestDto.getAmount().compareTo(BigDecimal.valueOf(pg.getAmount())) != 0) {
            throw new PaymentException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        applyEventFromPaymentData(userId, chargeReq, pg, EventSource.VERIFY);
    }

    private void applyEventFromPaymentData(Long userId,
        PointChargeRequestVO chargeReq,
        PaymentData pg,
        EventSource source) {

        final String impUid = pg.getImpUid();
        final String merchantUid = pg.getMerchantUid();
        final BigDecimal paidAmt = BigDecimal.valueOf(pg.getAmount());
        final String statusRaw = pg.getStatus();

        final PaymentStatus next = PaymentStatus.fromWebhook(statusRaw);           // 내부 상태
        final PaymentEventType evt = PaymentEventType.fromWebhook(statusRaw);      // 로그/멱등

        final Long ts =
            ("cancelled".equalsIgnoreCase(statusRaw) || "canceled".equalsIgnoreCase(statusRaw))
                ? pg.getCancelledAt()
                : pg.getPaidAt();
        final LocalDateTime pgEventAt = (ts != null)
            ? Instant.ofEpochSecond(ts).atZone(ZoneId.systemDefault()).toLocalDateTime()
            : LocalDateTime.now();

        // 1) 멱등 (payment_event 유니크)
        if (paymentEventRepository.existsByMerchantUidAndEventTypeAndPgEventAt(
            merchantUid, evt.name(), pgEventAt)) {
            log.info("Duplicate event ignored: {} {} {}", merchantUid, evt.name(), pgEventAt);
            return;
        }

        // 2) 현재 Payment
        PaymentVO cur = paymentRepository.findByImpUid(impUid);

        // 3) LWW: 기존 last_event_at보다 최신일 때만 갱신
        if (cur != null && cur.getLastEventAt() != null && !pgEventAt.isAfter(
            cur.getLastEventAt())) {
            saveEventLog(merchantUid, impUid, evt.name(), pgEventAt, source, pg);
            return;
        }

        // 4) 상태 전이 + 부수효과
        switch (next) {
            case SUCCESS: // paid
                if (chargeReq.getAmount().compareTo(paidAmt) != 0) {
                    saveEventLog(merchantUid, impUid, "FAILED_AMOUNT_MISMATCH", pgEventAt, source,
                        pg);
                    throw new PaymentException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
                }

                if (cur == null || cur.getStatus() != PaymentStatus.SUCCESS) {
                    // 포인트 적립
                    pointService.chargePoint(userId, paidAmt, impUid);

                    // 원요청에 영수증/시각 박제
                    chargeReq.setImpUid(impUid);
                    chargeReq.setPaidAt(
                        Instant.ofEpochSecond(pg.getPaidAt())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                    );
                    pointChargeRequestService.updateStatusToPaid(chargeReq);
                }

                if (cur == null) {
                    cur = PaymentVO.builder()
                        .impUid(impUid)
                        .merchantUid(merchantUid)
                        .userId(userId)
                        .amount(paidAmt)
                        .status(PaymentStatus.SUCCESS)
                        .paidAt(Instant.ofEpochSecond(pg.getPaidAt())
                            .atZone(ZoneId.systemDefault()).toLocalDateTime())
                        .build();
                } else {
                    cur.setStatus(PaymentStatus.SUCCESS);
                    cur.setAmount(paidAmt);
                    cur.setPaidAt(Instant.ofEpochSecond(pg.getPaidAt())
                        .atZone(ZoneId.systemDefault()).toLocalDateTime());
                }

                cur.setLastEventAt(pgEventAt);
                cur.setLastEventSource(source.name());
                cur.setVersion(cur.getVersion() == null ? 1 : cur.getVersion() + 1);

                if (cur.getPaymentId() == null) {
                    paymentRepository.save(cur);
                } else {
                    paymentRepository.update(cur);
                }
                break;

            case CANCELLED:
                // 부분취소는 스킵(나중에 처리)
                if (evt == PaymentEventType.PARTIAL_CANCEL) {
                    log.info(
                        "Partial cancel received. Refund is not processed. merchantUid={}, impUid={}",
                        merchantUid, impUid);
                    saveEventLog(merchantUid, impUid, "PARTIAL_CANCEL_SKIPPED", pgEventAt, source,
                        pg);
                    break;
                }

                if (cur != null && cur.getStatus() == PaymentStatus.SUCCESS) {
                    // 1) 포인트 회수
                    pointService.requestRefund(userId, cur.getAmount());
                    // 2) 원요청 취소로
                    pointChargeRequestService.updateStatusToCancelled(chargeReq);
                    // 3) payment 최신화
                    cur.setStatus(PaymentStatus.CANCELLED);
                    cur.setCanceledAt(pgEventAt);
                    cur.setLastEventAt(pgEventAt);
                    cur.setLastEventSource(source.name());
                    cur.setVersion(cur.getVersion() == null ? 1 : cur.getVersion() + 1);
                    paymentRepository.update(cur);
                }
                break;

            case FAILED:
            case EXPIRED:
            default: // 기타는 실패로 처리
                if (cur == null) {
                    cur = PaymentVO.builder()
                        .impUid(impUid)
                        .merchantUid(merchantUid)
                        .userId(userId)
                        .amount(paidAmt)
                        .status(PaymentStatus.FAILED)
                        .build();
                    cur.setLastEventAt(pgEventAt);
                    cur.setLastEventSource(source.name());
                    cur.setVersion(1L);
                    paymentRepository.save(cur);
                } else {
                    cur.setStatus(PaymentStatus.FAILED);
                    cur.setLastEventAt(pgEventAt);
                    cur.setLastEventSource(source.name());
                    cur.setVersion(cur.getVersion() == null ? 1 : cur.getVersion() + 1);
                    paymentRepository.update(cur);
                }
                break;
        }

// 5) 이벤트 로그 (멱등 키)
        saveEventLog(merchantUid, impUid, evt.name(), pgEventAt, source, pg);

    }

    // PaymentService
    private void saveEventLog(
        String merchantUid,
        String impUid,
        String eventType,
        LocalDateTime pgEventAt,
        EventSource source,
        org.bobj.payment.dto.PortOnePaymentResponse.PaymentData pg
    ) {
        try {
            String payload = null;
            try {
                // ObjectMapper 주입 안 했으면 일단 주석/삭제해도 됨 (JSON 저장 생략)
                // payload = objectMapper.writeValueAsString(pg);
            } catch (Exception ignore) {
            }

            log.info("event-log try: merchantUid={}, impUid={}, type={}, at={}, source={}",
                merchantUid, impUid, eventType, pgEventAt, source);

            paymentEventRepository.save(
                PaymentEventVO.builder()
                    .merchantUid(merchantUid)
                    .impUid(impUid)
                    .eventType(eventType)     // 예: "PAID" | "CANCELED" | "PARTIAL_CANCEL_SKIPPED"
                    .pgEventAt(pgEventAt)
                    .source(source.name())    // "VERIFY" | "WEBHOOK"
                    .payloadJson(payload)     // JSON 넣으려면 위에서 직렬화
                    .build()
            );
        } catch (Exception e) {
            log.debug("event log save skipped: {}", e.getMessage());
            log.error("event log save failed", e); // ← 스택트레이스 보이게
        }
    }

}
