package org.bobj.funding.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.bobj.funding.domain.FundingOrderVO;
import org.bobj.funding.mapper.FundingMapper;
import org.bobj.funding.mapper.FundingOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundingOrderService {
    private final FundingOrderMapper fundingOrderMapper;
    private final FundingMapper fundingMapper;

    // 주문 추가
    @Transactional
    public void createFundingOrder(FundingOrderVO vo) {
        fundingOrderMapper.insertFundingOrder(vo);
        BigDecimal orderPrice = vo.getOrderPrice();
        fundingMapper.increaseCurrentAmount(vo.getFundingId(),orderPrice);
    }

    // 주문 취소
    @Transactional
    public void refundFundingOrder(Long orderId, Long fundingId, BigDecimal orderPrice) {
        fundingOrderMapper.refundFundingOrder(orderId);
        fundingMapper.decreaseCurrentAmount(fundingId,orderPrice);
    }
}
