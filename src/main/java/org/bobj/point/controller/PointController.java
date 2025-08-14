package org.bobj.point.controller;

import io.swagger.annotations.*;
import java.math.BigDecimal;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.common.exception.ErrorResponse;
import org.bobj.common.response.ApiCommonResponse;
import org.bobj.payment.dto.WebhookDto;
import org.bobj.payment.service.PaymentService;
import org.bobj.payment.service.PaymentService.EventSource;
import org.bobj.point.RefundRequestDto;
import org.bobj.point.domain.PointTransactionVO;
import org.bobj.point.service.PointService;
import org.bobj.user.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Api(tags = "포인트 API ")
public class PointController {

    private final PointService pointService;
    private final PaymentService paymentService;

    /**
     * 포인트 입출금 내역 조회
     * GET /api/auth/point/transactions?userId=1
     */
    @GetMapping("/auth/point/transactions")
    @ApiOperation(
        value = "포인트 입출금 내역 조회",
        notes = "인증된 사용자의 포인트 입출금 내역을 반환합니다."
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "조회 성공", response = ApiCommonResponse.class),
        @ApiResponse(code = 401, message = "인증 필요", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<List<PointTransactionVO>>> getTransactions(
        @ApiIgnore @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal.getUserId();
        List<PointTransactionVO> transactions = pointService.findTransactionsByUserId(userId);
        return ResponseEntity.ok(ApiCommonResponse.createSuccess(transactions));
    }

    /**
     * 현재 포인트 보유량 조회
     * GET /api/point/balance?userId=1
     */
    @GetMapping("/auth/point/balance")
    @ApiOperation(
        value = "현재 포인트 보유량 조회",
        notes = "인증된 사용자의 현재 포인트 보유량을 반환합니다."
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "조회 성공", response = ApiCommonResponse.class),
        @ApiResponse(code = 401, message = "인증 필요", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<BigDecimal>> getPointBalance(
        @ApiIgnore @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal.getUserId();
        BigDecimal balance = pointService.getTotalPoint(userId);
        return ResponseEntity.ok(ApiCommonResponse.createSuccess(balance));
    }

    /**
     * 포인트 환급 요청
     * POST /api/point/refund?userId=1
     * Body: { "amount": 5000 }
     */
    @PostMapping("/auth/point/refund")
    @ApiOperation(
        value = "포인트 환급 요청",
        notes = "인증된 사용자의 포인트 환급을 요청합니다."
    )
    @ApiResponses({
        @ApiResponse(code = 200, message = "환급 요청 성공", response = ApiCommonResponse.class),
        @ApiResponse(code = 400, message = "잘못된 요청 (잔액 부족 등)", response = ErrorResponse.class),
        @ApiResponse(code = 401, message = "인증 필요", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<String>> requestRefund(
        @ApiIgnore @AuthenticationPrincipal UserPrincipal principal,
        @RequestBody @Valid RefundRequestDto refundRequestDto
    ) {
        Long userId = principal.getUserId();
        pointService.requestRefund(userId, refundRequestDto.getAmount());
        return ResponseEntity.ok(ApiCommonResponse.createSuccess("환급 요청이 완료되었습니다."));
    }

    // org.bobj.point.controller.PointController (웹훅 부분만)
    @PostMapping("/point/webhook")
    public ResponseEntity<String> webhook(@RequestBody WebhookDto dto) {
        log.info("Webhook received: {}", dto);

        // 1) 필수 필드 가드(imp_uid 없으면 포트원 재시도만 늘림)
        if (dto == null || dto.getImpUid() == null || dto.getImpUid().isBlank()) {
            log.warn("Webhook ignored: imp_uid is missing. payload={}", dto);
            return ResponseEntity.ok("ignored"); // 2xx로 마무리(재시도 방지)
        }

        try {
            // 2) 포트원 단건 조회 + 적용
            paymentService.verifyWithPortOneAndApply(dto, EventSource.WEBHOOK);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            // 3) 비즈니스 오류라도 2xx로 흡수(포트원은 2xx면 성공으로 간주함)
            log.error("Webhook handle error: {}", e.getMessage(), e);
            return ResponseEntity.ok("received");
        }
    }

}
