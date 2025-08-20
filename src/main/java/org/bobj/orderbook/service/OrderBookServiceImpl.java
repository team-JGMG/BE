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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class OrderBookServiceImpl implements OrderBookService{

    private final FundingMapper fundingMapper;
    private final TradeMapper tradeMapper;
    private final OrderMapper orderMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    // 상한가/하한가 계산을 위한 비율
    private static final BigDecimal LIMIT_PERCENTAGE = new BigDecimal("0.30"); // 30%

    @Transactional(readOnly = true)
    @Override
    public OrderBookResponseDTO getOrderBookByFundingId(Long fundingId) {

        String cacheKey = "orderBook:" + fundingId;

        // 1.Redis 캐시에서 데이터 조회
        OrderBookResponseDTO cachedOrderBook = (OrderBookResponseDTO) redisTemplate.opsForValue().get(cacheKey);
        if (cachedOrderBook != null) {
            return cachedOrderBook;
        }


        // 2. 캐시 미스 시, 기존 로직으로 호가창 계산
        // 현재가 계산 (가장 최근의 체결 가격 조회)
        BigDecimal latestTradePrice = tradeMapper.findLatestTradePriceByFundingId(fundingId);
        BigDecimal currentPrice;
        if (latestTradePrice != null) {
            currentPrice = latestTradePrice;
        } else {
            FundingDetailResponseDTO funding = Optional.ofNullable(fundingMapper.findFundingById(fundingId))
                    .orElseThrow(() -> new IllegalArgumentException("funding id에 대한 펀딩이 존재하지 않습니다."));
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

        OrderBookResponseDTO orderBook = OrderBookResponseDTO.builder()
                .currentPrice(currentPrice)
                .upperLimitPrice(upperLimitPrice)
                .lowerLimitPrice(lowerLimitPrice)
                .buyOrders(buyOrders)
                .sellOrders(sellOrders)
                .timestamp(LocalDateTime.now())
                .build();

        // 4. 계산된 데이터를 캐시에 저장 (예: 30초 TTL 설정)
        redisTemplate.opsForValue().set(cacheKey, orderBook, 30, TimeUnit.SECONDS);

        return orderBook;
    }

    @Override
    public void evictOrderBookCache(Long fundingId) {
        String cacheKey = "orderBook:" + fundingId;
        redisTemplate.delete(cacheKey);
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
