package org.bobj.trade.service;

import io.swagger.annotations.ApiModelProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.trade.domain.TradeVO;
import org.bobj.trade.dto.DailyTradeHistoryDTO;
import org.bobj.trade.dto.request.TradeHistoryRequestDTO;
import org.bobj.trade.dto.response.FundingTradeHistoryResponseDTO;
import org.bobj.trade.mapper.TradeMapper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
@RequiredArgsConstructor
public class TradeHistoryServiceImpl implements TradeHistoryService{

    private final TradeMapper tradeMapper;

    // 기본 조회 개수 (60개)
    private static final int DEFAULT_HISTORY_COUNT = 60;
    // 기본 조회 기간을 계산할 때 사용될 최소 백업 기간 (예: 6개월)
    // 60개의 거래일 데이터를 확보하기 위해 충분히 넓은 기간을 설정합니다.
    private static final long DEFAULT_LOOKBACK_MONTHS = 6;

    @Transactional(readOnly = true)
    public FundingTradeHistoryResponseDTO getDailyTradeHistory(Long fundingId, TradeHistoryRequestDTO requestDTO) {
        return null;
    }
}
