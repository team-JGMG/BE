package org.bobj.point.controller;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.common.exception.ErrorResponse;
import org.bobj.common.response.ApiCommonResponse;
import org.bobj.payment.dto.VerifyRequestDto;
import org.bobj.payment.service.PaymentService;
import org.bobj.point.domain.PointChargeRequestVO;
import org.bobj.point.service.PointChargeRequestService;
import org.bobj.point.util.MerchantUidGenerator;
import org.bobj.user.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // ★ 추가
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import springfox.documentation.annotations.ApiIgnore;

// (프로젝트 내) 인증 사용자 객체

@RestController
@RequestMapping("/api/auth/point")
@RequiredArgsConstructor
@Log4j2
@Api(tags = "포인트 충전 및 검증 API")
public class PointChargeRequestController {

    private final PointChargeRequestService pointChargeRequestService;
    private final PaymentService paymentService;

    @PostMapping("/charge")
    @ApiOperation(value = "포인트 충전 요청", notes = "사용자가 지정한 금액으로 포인트 충전 요청을 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "충전 요청 성공", response = ApiCommonResponse.class),
        @ApiResponse(code = 400, message = "잘못된 요청 (금액 누락 등)", response = ErrorResponse.class),
        @ApiResponse(code = 401, message = "인증 필요", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<String>> createCharge(
        @RequestParam @ApiParam(value = "충전 금액", example = "5000", required = true) BigDecimal amount,
        @ApiIgnore @AuthenticationPrincipal UserPrincipal principal // ★ 변경
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body((ApiCommonResponse<String>) ApiCommonResponse.createError("인증이 필요합니다."));
        }

        Long userId = principal.getUserId(); // ★ 변경
        String merchantUid = MerchantUidGenerator.generate(userId);

        PointChargeRequestVO request = PointChargeRequestVO.builder()
            .userId(userId)
            .amount(amount)
            .merchantUid(merchantUid)
            .status("PENDING")
            .build();

        pointChargeRequestService.createChargeRequest(request);
        log.info("포인트 충전 요청 생성: userId={}, amount={}, merchantUid={}", userId, amount, merchantUid);

        return ResponseEntity.ok(ApiCommonResponse.createSuccess(merchantUid));
    }

    @PostMapping("/verify")
    @ApiOperation(value = "결제 검증 및 포인트 지급", notes = "imp_uid를 통해 결제를 검증하고, 결제가 성공한 경우 포인트를 지급합니다.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "포인트 충전 성공", response = ApiCommonResponse.class),
        @ApiResponse(code = 400, message = "잘못된 요청 (금액 불일치, 결제 실패, 중복 처리 등)", response = ErrorResponse.class),
        @ApiResponse(code = 401, message = "인증 필요", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<String>> verifyPayment(
        @RequestBody @ApiParam(value = "결제 검증 요청 DTO", required = true) VerifyRequestDto requestDto,
        @ApiIgnore @AuthenticationPrincipal UserPrincipal principal // ★ 변경
    ) {
//        if (principal == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                .body((ApiCommonResponse<String>) ApiCommonResponse.createError("인증이 필요합니다."));
//        }


        Long userId = (principal != null) ? principal.getUserId() : null; // 💡 principal 없어도 통과
        paymentService.verifyPayment(userId, requestDto);
        log.info("결제 검증 요청: userId={}, impUid={}", userId, requestDto.getImpUid());

        paymentService.verifyPayment(userId, requestDto);

        return ResponseEntity.ok(ApiCommonResponse.createSuccess("포인트 충전 성공"));
    }
}
