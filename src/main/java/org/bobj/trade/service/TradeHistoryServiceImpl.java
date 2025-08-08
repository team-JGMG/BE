package org.bobj.trade.service;

import io.swagger.annotations.ApiModelProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.funding.mapper.FundingMapper;
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
import java.util.ArrayList;
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
    private final FundingMapper fundingMapper;

    @Transactional(readOnly = true)
    @Override
    public FundingTradeHistoryResponseDTO getDailyTradeHistory(Long fundingId, TradeHistoryRequestDTO requestDTO) {
        // 펀딩 존재 여부 확인 (필수)
        if (fundingMapper.findFundingById(fundingId) == null) {

            throw new IllegalArgumentException("funding id에 대한 펀딩이 존재하지 않습니다.");
        }

        LocalDate today = LocalDate.now();
        LocalDate maxAllowedDate = today.minusDays(1);
        LocalDate requestedEndDate = requestDTO.getEndDate();

        if (requestedEndDate.isAfter(maxAllowedDate)) {
            throw new IllegalArgumentException("체결 내역은 현재 날짜 전날까지만 조회할 수 있습니다. (요청 endDate: "
                    + requestedEndDate + ", 최대 허용: " + maxAllowedDate + ")");
        }

        // 일별 체결 요약 데이터 조회
        List<DailyTradeHistoryDTO> dailySummaries = tradeMapper.findDailyTradeSummary(fundingId, requestDTO.getStartDate(), requestDTO.getEndDate());

        // 변화율 계산
        List<DailyTradeHistoryDTO> historyWithChangeRate = calculateChangeRates(dailySummaries);

        return FundingTradeHistoryResponseDTO.builder()
                .fundingId(fundingId)
                .history(historyWithChangeRate)
                .build();
    }

    //일별 체결 내역 리스트에 전일 대비 변화율을 계산
    private List<DailyTradeHistoryDTO> calculateChangeRates(List<DailyTradeHistoryDTO> dailySummaries) {
        if (dailySummaries == null || dailySummaries.isEmpty()) {
            return new ArrayList<>();
        }

        List<DailyTradeHistoryDTO> result = new ArrayList<>();
        BigDecimal previousPrice = null;


        for (DailyTradeHistoryDTO current : dailySummaries) {
            BigDecimal currentPrice = current.getClosingPrice(); // 현재 날짜의 종가

            // 1. 단일 변화율 계산
            BigDecimal changeRate = calculateSingleChangeRate(currentPrice, previousPrice);

            result.add(DailyTradeHistoryDTO.builder()
                    .date(current.getDate())
                    .closingPrice(current.getClosingPrice())
                    .volume(current.getVolume())
                    .priceChangeRate(changeRate)
                    .build());

            previousPrice = currentPrice; // 현재 가격을 다음 반복을 위한 이전 가격으로 설정

        }
        return result;
    }

    private BigDecimal calculateSingleChangeRate(BigDecimal currentPrice, BigDecimal previousPrice) {
        // 이전 가격이 없거나 0일 경우, 변화율은 0으로 간주
        if (previousPrice == null || previousPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // (현재가 - 전일가) / 전일가 * 100
        return currentPrice.subtract(previousPrice)
                .divide(previousPrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP); // 최종 변화율 소수점 2자리까지
    }

}

