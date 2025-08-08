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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/point")
@RequiredArgsConstructor
@Api(tags = "포인트 API (테스트용)")
public class PointController {

    private final PointService pointService;

    @GetMapping("/transactions")
    @ApiOperation(value = "포인트 입출금 내역 조회 (테스트용)", notes = "userId를 쿼리 파라미터로 받아 테스트합니다.")
    public ResponseEntity<ApiCommonResponse<List<PointTransactionVO>>> getTransactionsForTest(
        @ApiParam(value = "사용자 ID", required = true, example = "1")
        @RequestParam(name = "userId") Long userId
    ) {
        List<PointTransactionVO> transactions = pointService.findTransactionsByUserId(userId);
        return ResponseEntity.ok(ApiCommonResponse.createSuccess(transactions));
    }

    @GetMapping("/balance")
    @ApiOperation(value = "현재 포인트 보유량 조회 (테스트용)", notes = "userId를 쿼리 파라미터로 받아 테스트합니다.")
    public ResponseEntity<ApiCommonResponse<BigDecimal>> getPointBalanceForTest(
        @ApiParam(value = "사용자 ID", required = true, example = "1")
        @RequestParam(name = "userId") Long userId
    ) {
        BigDecimal balance = pointService.getTotalPoint(userId);
        return ResponseEntity.ok(ApiCommonResponse.createSuccess(balance));
    }

    @PostMapping("/refund")
    @ApiOperation(value = "포인트 환급 요청 (테스트용)", notes = "userId를 쿼리 파라미터로 받아 테스트합니다.")
    @ApiResponses({
        @ApiResponse(code = 200, message = "환급 요청 성공", response = ApiCommonResponse.class),
        @ApiResponse(code = 400, message = "잘못된 요청 (잔액 부족 등)", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<String>> requestRefundForTest(
        @ApiParam(value = "사용자 ID", required = true, example = "1")
        @RequestParam(name = "userId") Long userId,
        @RequestBody @Valid RefundRequestDto refundRequestDto
    ) {
        pointService.requestRefund(userId, refundRequestDto.getAmount());
        return ResponseEntity.ok(ApiCommonResponse.createSuccess("환급 요청이 완료되었습니다."));
    }
}
