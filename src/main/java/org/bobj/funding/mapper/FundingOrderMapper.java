package org.bobj.funding.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bobj.funding.domain.FundingOrderVO;
import org.bobj.funding.dto.FundingOrderUserResponseDTO;

import java.util.List;

@Mapper
public interface FundingOrderMapper {
    // 펀딩 주문 생성
    void insertFundingOrder(FundingOrderVO fundingOrder);

    // 주문 취소
    void refundFundingOrder(@Param("orderId") Long orderId);

    // 내가 투자한 주문 리스트
    List<FundingOrderUserResponseDTO> findFundingOrdersByUserId(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit);
}
