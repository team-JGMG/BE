package org.bobj.trade.mapper;

import org.apache.ibatis.annotations.Param;
import org.bobj.trade.domain.TradeVO;
import org.bobj.trade.dto.DailyTradeHistoryDTO;

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

    /**
     * 특정 펀딩에 대한 일별 체결 내역을 조회합니다.
     * 각 날짜의 종가(마지막 거래 가격)와 총 거래량을 집계
     */
    List<DailyTradeHistoryDTO> findDailyTradeSummary(
            @Param("fundingId") Long fundingId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}

