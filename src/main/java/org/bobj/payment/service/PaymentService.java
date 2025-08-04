package org.bobj.payment.service;


import static org.bobj.payment.domain.PaymentStatus.FAILED;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bytecode.Throw;
import org.bobj.common.constants.ErrorCode;
import org.bobj.payment.client.PortOneClient;
import org.bobj.payment.domain.PaymentStatus;
import org.bobj.payment.domain.PaymentVO;
import org.bobj.payment.dto.PortOnePaymentResponse;
import org.bobj.payment.dto.PortOnePaymentResponse.PaymentData;
import org.bobj.payment.dto.VerifyRequestDto;
import org.bobj.payment.exception.PaymentException;
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
    private final PointService pointService; // 포인트 적립 처리용
    private final PointChargeRequestService pointChargeRequestService;


    @Transactional
    public void verifyPayment(Long userId, VerifyRequestDto requestDto) {
        String impUid = requestDto.getImpUid();
        BigDecimal requestAmount = requestDto.getAmount();
        PaymentData paymentData = null;

        try {
            // 1. 포트원에서 결제 정보 조회
            PortOnePaymentResponse response = portOneClient.getPaymentInfo(impUid);
            paymentData = response.getResponse();

            if (paymentData == null) {
                throw new PaymentException(ErrorCode.PAYMENT_NOT_FOUND);
            }

            // 2. 요청 내역 조회 (merchant_uid 기준)
            PointChargeRequestVO chargeRequest =
                pointChargeRequestService.findByMerchantUid(paymentData.getMerchantUid());

            if (chargeRequest == null) {
                throw new PaymentException(ErrorCode.PAYMENT_REQUEST_NOT_FOUND);
            }

            // 3. 결제 금액 일치 확인
            if (BigDecimal.valueOf(paymentData.getAmount()).compareTo(requestAmount) != 0) {
                throw new PaymentException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
            }

            // 4. 결제 상태 확인
            if (!"paid".equalsIgnoreCase(paymentData.getStatus())) {
                throw new PaymentException(ErrorCode.PAYMENT_NOT_COMPLETED);
            }

            // 5. 중복 결제 방지
            if (paymentRepository.existsByImpUid(impUid)) {
                throw new PaymentException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
            }

            // 6. 상태 업데이트 & 포인트 지급
            chargeRequest.setStatus("PAID");
            chargeRequest.setImpUid(impUid);
            pointChargeRequestService.updateStatusToPaid(chargeRequest);

            pointService.chargePoint(userId, requestAmount, impUid);

            // 7. 결제 정보 저장
            paymentRepository.save(PaymentVO.builder()
                .impUid(impUid)
                .merchantUid(paymentData.getMerchantUid())
                .userId(userId)
                .amount(BigDecimal.valueOf(paymentData.getAmount()))
                .status(PaymentStatus.SUCCESS)
                .paidAt(Instant.ofEpochSecond(paymentData.getPaidAt())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime())
                .build());

        } catch (PaymentException e) {
            // ❗검증 실패한 경우에도 로그 & 저장
            log.error("[결제 실패] {} - {}", impUid, e.getErrorCode().name());

            // 실패 정보도 저장 (중복 방지 필요)
            if (!paymentRepository.existsByImpUid(impUid)) {
                paymentRepository.save(PaymentVO.builder()
                    .impUid(impUid)
                    .merchantUid(paymentData != null ? paymentData.getMerchantUid() : null)
                    .userId(userId)
                    .amount(paymentData != null ? BigDecimal.valueOf(paymentData.getAmount()) : requestAmount)
                    .status(FAILED)
                    .paidAt(null)
                    .build());
            }

            throw e; // 다시 던져서 ControllerAdvice에서 처리되도록
        }
    }



}
