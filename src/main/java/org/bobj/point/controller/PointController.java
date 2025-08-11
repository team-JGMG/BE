package org.bobj.point.controller;

import io.swagger.annotations.*;
import java.math.BigDecimal;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bobj.common.exception.ErrorResponse;
import org.bobj.common.response.ApiCommonResponse;
import org.bobj.point.RefundRequestDto;
import org.bobj.point.domain.PointTransactionVO;
import org.bobj.point.service.PointService;
import org.bobj.user.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/api/auth/point")
@RequiredArgsConstructor
@Api(tags = "포인트 API ")
public class PointController {

    private final PointService pointService;

    /**
     * 포인트 입출금 내역 조회
     * GET /api/auth/point/transactions?userId=1
     */
    @GetMapping("/transactions")
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
    @GetMapping("/balance")
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
    @PostMapping("/refund")
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
}
