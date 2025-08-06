package org.bobj.point.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.function.LongPredicate;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bobj.common.exception.ErrorResponse;
import org.bobj.common.response.ApiCommonResponse;
import org.bobj.point.RefundRequestDto;
import org.bobj.point.domain.PointTransactionVO;
import org.bobj.point.service.PointService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/point")
@RequiredArgsConstructor
public class PointController {
    private final PointService pointService;
    @GetMapping("/transactions")
    @ApiOperation(value = "포인트 입출금 내역 조회", notes = "로그인한 사용자의 포인트 입출금 트랜잭션 내역을 조회합니다.")
    public ResponseEntity<ApiCommonResponse<List<PointTransactionVO>>> getTransactions(Principal principal){
        Long userId = Long.parseLong(principal.getName());
        List<PointTransactionVO> transactions = pointService.findTransactionsByUserId(userId);
        return ResponseEntity.ok(ApiCommonResponse.createSuccess(transactions));
    }


    @GetMapping("/balance")
    @ApiOperation(value = "현재 포인트 보유량 조회", notes = "로그인한 사용자의 현재 포인트 보유량을 반환합니다.")
    public ResponseEntity<ApiCommonResponse<BigDecimal>> getPointBalance(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        BigDecimal balance = pointService.getTotalPoint(userId);
        return ResponseEntity.ok(ApiCommonResponse.createSuccess(balance));
    }



    @PostMapping("/refund")
    @ApiOperation(value = "포인트 환급 요청", notes = "사용자가 입력한 금액만큼 포인트를 환급 요청합니다.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "환급 요청 성공", response = ApiCommonResponse.class),
        @ApiResponse(code = 400, message = "잘못된 요청 (잔액 부족 등)", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<String>> requestRefund(
        @RequestBody RefundRequestDto refundRequestDto,
        Principal principal
    ) {
        Long userId = Long.parseLong(principal.getName());
        pointService.requestRefund(userId, refundRequestDto.getAmount());
        return ResponseEntity.ok(ApiCommonResponse.createSuccess("환급 요청이 완료되었습니다."));
    }




}
