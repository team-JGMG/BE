package org.bobj.point.controller;


import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.bobj.point.domain.PointChargeRequestVO;
import org.bobj.point.service.PointChargeRequestService;
import org.bobj.point.util.MerchantUidGenerator;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/point")
@RequiredArgsConstructor
public class PointChargeRequestController {

    private final PointChargeRequestService pointChargeRequestService;

    @PostMapping("/charge")
    public String createCharge(@RequestParam Long userId, @RequestParam BigDecimal amount){
        String merchantUid = MerchantUidGenerator.generate(userId);

        PointChargeRequestVO request = PointChargeRequestVO.builder()
            .userId(userId)
            .amount(amount)
            .merchantUid(merchantUid)
            .status("PENDING")
            .build();

        pointChargeRequestService.createChargeRequest(request);
        return merchantUid;
    }


}
