package org.bobj.funding.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.common.dto.CustomSlice;
import org.bobj.funding.domain.FundingOrderVO;
import org.bobj.funding.dto.FundingOrderUserResponseDTO;
import org.bobj.funding.mapper.FundingMapper;
import org.bobj.funding.mapper.FundingOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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

    // 내가 투자한 주문 리스트
    public CustomSlice<FundingOrderUserResponseDTO> getFundingOrderUsers(Long userId, String status, int page, int size) {
        int offset = page*size;
        String upperStatus = status.toUpperCase();

        List<FundingOrderUserResponseDTO> content = fundingOrderMapper.findFundingOrdersByUserId(userId,upperStatus,offset,size+1);
        boolean hasNext = content.size() > size;
        if (hasNext) {
            content.remove(size);
        }

        return new CustomSlice<>(content,hasNext);
    }
}
