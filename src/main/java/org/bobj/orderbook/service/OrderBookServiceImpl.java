package org.bobj.orderbook.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.common.ActiveSubscriptionsChecker;
import org.bobj.funding.domain.FundingVO;
import org.bobj.funding.dto.FundingDetailResponseDTO;
import org.bobj.funding.mapper.FundingMapper;
import org.bobj.order.domain.OrderType;
import org.bobj.order.domain.OrderVO;
import org.bobj.order.mapper.OrderMapper;
import org.bobj.orderbook.dto.OrderBookEntryDTO;
import org.bobj.orderbook.dto.response.OrderBookResponseDTO;
import org.bobj.property.domain.PropertyVO;
import org.bobj.property.mapper.PropertyMapper;
import org.bobj.trade.domain.TradeVO;
import org.bobj.trade.mapper.TradeMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class OrderBookServiceImpl implements OrderBookService{

    private final PropertyMapper propertyMapper;
    private final FundingMapper fundingMapper;
    private final TradeMapper tradeMapper;
    private final OrderMapper orderMapper;


    // 상한가/하한가 계산을 위한 비율
    private static final BigDecimal LIMIT_PERCENTAGE = new BigDecimal("0.30"); // 30%
    private final ActiveSubscriptionsChecker subscriptionsChecker;

    @Transactional(readOnly = true)
    @Override
    public OrderBookResponseDTO getOrderBookByFundingId(Long fundingId) {

        //1. 펀딩 정보 조회
        FundingDetailResponseDTO funding = Optional.ofNullable(fundingMapper.findFundingById(fundingId)) // Mapper 사용
                .orElseThrow(() -> new IllegalArgumentException("funding id에 대한 펀딩이 존재하지 않습니다."));

        // 건물명 조회
//        PropertyVO property = Optional.ofNullable(propertyMapper.findById(funding.getPropertyId()))
//                .orElseThrow(() -> new IllegalArgumentException("funding id에 대한 건물이 존재하지 않습니다. " + fundingId));
//        String propertyTitle = property.getTitle();

        // 2. 현재가 계산 (가장 최근의 체결 가격 조회)  -- 이거 fundings테이블의 currentshareamount로 대체 할 수 있는지
        BigDecimal latestTradePrice = tradeMapper.findLatestTradePriceByFundingId(fundingId);

        BigDecimal currentPrice;
        if (latestTradePrice != null) {
            currentPrice = latestTradePrice;
        } else {
               currentPrice = funding.getCurrentShareAmount();
        }

        // 3. 상한가/하한가 계산
        BigDecimal upperLimitPrice = currentPrice.multiply(BigDecimal.ONE.add(LIMIT_PERCENTAGE))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal lowerLimitPrice = currentPrice.multiply(BigDecimal.ONE.subtract(LIMIT_PERCENTAGE))
                .setScale(2, RoundingMode.HALF_UP);

        // 4. 매수/매도 호가 잔량 집계
        List<OrderVO> activeOrders = orderMapper.findOrdersByFundingId(fundingId);


        // 매수 호가 집계 (가격 내림차순 정렬)
        List<OrderBookEntryDTO> buyOrders = buildOrderBook(activeOrders, OrderType.BUY, Comparator.reverseOrder());
        // 매도 호가 집계 (가격 오름차순 정렬)
        List<OrderBookEntryDTO> sellOrders = buildOrderBook(activeOrders, OrderType.SELL, Comparator.naturalOrder());

        return OrderBookResponseDTO.builder()
                .currentPrice(currentPrice)
                .upperLimitPrice(upperLimitPrice)
                .lowerLimitPrice(lowerLimitPrice)
                .buyOrders(buyOrders)
                .sellOrders(sellOrders)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private List<OrderBookEntryDTO> buildOrderBook(List<OrderVO> orders, OrderType type, Comparator<BigDecimal> sortOrder) {
        return orders.stream()
                .filter(order -> order.getOrderType() == type)
                .collect(Collectors.groupingBy(
                        OrderVO::getOrderPricePerShare,
                        Collectors.summingInt(OrderVO::getRemainingShareCount)
                ))
                .entrySet().stream()
                .map(entry -> OrderBookEntryDTO.builder()
                        .price(entry.getKey())
                        .quantity(entry.getValue())
                        .build())
                .sorted(Comparator.comparing(OrderBookEntryDTO::getPrice, sortOrder))
                .collect(Collectors.toList());
    }

}
