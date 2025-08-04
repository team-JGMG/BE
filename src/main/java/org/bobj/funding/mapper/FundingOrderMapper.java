package org.bobj.funding.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bobj.funding.domain.FundingOrderVO;
import org.bobj.funding.dto.FundingOrderLimitDTO;
import org.bobj.funding.dto.FundingOrderUserResponseDTO;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface FundingOrderMapper {
    // 펀딩 주문 생성
    void insertFundingOrder(@Param("userId") Long userId,
                            @Param("fundingId") Long fundingId,
                            @Param("shareCount") int shareCount,
                            @Param("orderPrice") BigDecimal orderPrice);

    // 주문 취소
    void refundFundingOrder(@Param("orderId") Long orderId);

    // 내가 투자한 주문 리스트
    List<FundingOrderUserResponseDTO> findFundingOrdersByUserId(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit);

    // 주문 가능 정보 조회
    FundingOrderLimitDTO findFundingOrderLimit(
            @Param("userId") Long userId,
            @Param("fundingId") Long fundingId);

    // 펀딩 ID에 해당하는 모든 주문 상태 변경
    void markOrdersAsSuccessByFundingId(@Param("fundingId") Long fundingId);

    // 펀딩 ID에 해당하는 모든 주문 조회
    List<FundingOrderVO> findAllOrdersByFundingId(Long fundingId);


    // 펀딩 ID에 해당하는 펀딩 주문 ID 리스트 조회
    List<Long> findFundingOrderIdsByFundingId(@Param("fundingId") Long fundingId);

    // 펀딩 주문 ID 리스트에 해당하는 펀딩 주문 데이터 status 수정
    void updateFundingOrderStatusToRefundedByOrderIds(@Param("orderIds") List<Long> orderIds);

}
