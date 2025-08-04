package org.bobj.point.controller;

import io.swagger.annotations.ApiOperation;
import java.security.Principal;
import java.util.List;
import java.util.function.LongPredicate;
import lombok.RequiredArgsConstructor;
import org.bobj.common.response.ApiCommonResponse;
import org.bobj.point.domain.PointTransactionVO;
import org.bobj.point.service.PointService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ResponseEntity<ApiCommonResponse<Long>> getPointBalance(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        Long balance = pointService.getTotalPoint(userId);
        return ResponseEntity.ok(ApiCommonResponse.createSuccess(balance));
    }


}
