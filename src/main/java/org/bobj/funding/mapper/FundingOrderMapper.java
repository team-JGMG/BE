package org.bobj.funding.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bobj.funding.domain.FundingOrderVO;

import java.util.List;

@Mapper
public interface FundingOrderMapper {
//    // 주문 단건 조회
//    FundingOrderVO findById(@Param("orderId") Long orderId);
//
//    // 특정 펀딩에 참여한 전체 주문 조회 (필요시 유저별 조회도 가능)
//    List<FundingOrderVO> findByFundingId(@Param("fundingId") Long fundingId);
//
//    // 특정 유저의 특정 펀딩 참여 내역 조회
//    List<FundingOrderVO> findByUserIdAndFundingId(@Param("userId") Long userId, @Param("fundingId") Long fundingId);

    // 펀딩 주문 생성
    void insertFundingOrder(FundingOrderVO fundingOrder);

    // 주문 취소
    void refundFundingOrder(@Param("orderId") Long orderId);
}
