package org.bobj.trade.mapper;

import org.apache.ibatis.annotations.Param;
import org.bobj.trade.domain.TradeVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TradeMapper {

    // 체결 내역을 저장
    void insert(TradeVO tradeVO);

    // 펀딩에 대한 거래 내역 날짜 범위로 조회
    // 일별 집계
    List<TradeVO> findTradesForHistory(
            @Param("fundingId") Long fundingId,
            @Param("startDate") LocalDate startDate, //조회 시작일
            @Param("endDate") LocalDate endDate //조회 종료
    );

    //가장 최근 체결 가격 조회
    BigDecimal findLatestTradePriceByFundingId(@Param("fundingId") Long fundingId);
}
