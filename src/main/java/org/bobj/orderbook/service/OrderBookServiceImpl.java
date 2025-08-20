package org.bobj.orderbook.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.funding.dto.FundingDetailResponseDTO;
import org.bobj.funding.mapper.FundingMapper;
import org.bobj.order.domain.OrderType;
import org.bobj.order.domain.OrderVO;
import org.bobj.order.mapper.OrderMapper;
import org.bobj.orderbook.dto.OrderBookEntryDTO;
import org.bobj.orderbook.dto.response.OrderBookResponseDTO;
import org.bobj.orderbook.dto.response.OrderBookUpdateDTO;
import org.bobj.trade.mapper.TradeMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class OrderBookServiceImpl implements OrderBookService{

    private final FundingMapper fundingMapper;
    private final TradeMapper tradeMapper;
    private final OrderMapper orderMapper;

    private final RedisTemplate<String, OrderBookResponseDTO> orderBookRedisTemplate;

    // 상한가/하한가 계산을 위한 비율
    private static final BigDecimal LIMIT_PERCENTAGE = new BigDecimal("0.30"); // 30%

    @Transactional(readOnly = true)
    @Override
    public OrderBookResponseDTO getOrderBookByFundingId(Long fundingId) {
        String cacheKey = "orderBook:" + fundingId;
        OrderBookResponseDTO cachedOrderBook = orderBookRedisTemplate.opsForValue().get(cacheKey);

        if (cachedOrderBook != null) {
            return cachedOrderBook;
        }

        // 캐시 미스 시, 호가창을 계산하고 캐시에 저장
        return calculateAndCacheOrderBook(fundingId);
    }

    @Transactional
    @Override
    public List<OrderBookUpdateDTO> updateOrderBookAndSendUpdates(Long fundingId) {
        String cacheKey = "orderBook:" + fundingId;

        // 1. Redis에서 이전 호가창 데이터 가져오기
        OrderBookResponseDTO oldOrderBook = orderBookRedisTemplate.opsForValue().get(cacheKey);

        // 2. 현재 최신 호가창 데이터 계산
        OrderBookResponseDTO newOrderBook = calculateAndCacheOrderBook(fundingId);

        // 3. 업데이트 DTO를 담을 리스트
        List<OrderBookUpdateDTO> updates = new ArrayList<>();

        // 이전 데이터가 없으면 전체 호가창을 업데이트로 간주
        if (oldOrderBook == null) {
            updates.add(OrderBookUpdateDTO.builder()
                    .type(OrderBookUpdateDTO.Type.FULL)
                    .buyOrders(newOrderBook.getBuyOrders())
                    .sellOrders(newOrderBook.getSellOrders())
                    .currentPrice(newOrderBook.getCurrentPrice())
                    .build());

            return updates;
        }

        // 4. 매수/매도 호가 변경분 비교
        updates.addAll(findUpdates(oldOrderBook.getBuyOrders(), newOrderBook.getBuyOrders(), OrderType.BUY));
        updates.addAll(findUpdates(oldOrderBook.getSellOrders(), newOrderBook.getSellOrders(), OrderType.SELL));

        // 5. 현재가 변경분 추가(거래 체결 시에만 변동)
        if (oldOrderBook.getCurrentPrice().compareTo(newOrderBook.getCurrentPrice()) != 0) {
            updates.add(OrderBookUpdateDTO.builder()
                    .type(OrderBookUpdateDTO.Type.PRICE_UPDATE)
                    .currentPrice(newOrderBook.getCurrentPrice())
                    .build());
        }

        return updates;
    }

    private Map<BigDecimal, Integer> buildOrderBookMap(List<OrderBookEntryDTO> entries) {
        return entries.stream()
                .collect(Collectors.toMap(OrderBookEntryDTO::getPrice, OrderBookEntryDTO::getQuantity));
    }


    // 이전 데이터와 새 데이터를 비교하여 변경분을 찾음
    private List<OrderBookUpdateDTO> findUpdates(List<OrderBookEntryDTO> oldEntries, List<OrderBookEntryDTO> newEntries, OrderType type) {
        List<OrderBookUpdateDTO> updates = new ArrayList<>();
        Map<BigDecimal, Integer> oldMap = buildOrderBookMap(oldEntries);
        Map<BigDecimal, Integer> newMap = buildOrderBookMap(newEntries);

        // 새로운 항목/변경된 항목 찾기
        for (Map.Entry<BigDecimal, Integer> newEntry : newMap.entrySet()) {
            BigDecimal price = newEntry.getKey();
            Integer newQuantity = newEntry.getValue();
            Integer oldQuantity = oldMap.get(price);

            // 새로운 가격이 추가되었거나 수량이 변경된 경우
            if (oldQuantity == null || !oldQuantity.equals(newQuantity)) {
                updates.add(OrderBookUpdateDTO.builder()
                        .type(oldQuantity == null ? OrderBookUpdateDTO.Type.ADD : OrderBookUpdateDTO.Type.UPDATE)
                        .price(price)
                        .quantity(BigDecimal.valueOf(newQuantity))
                        .build());
            }
        }

        // 삭제된 항목 찾기
        for (Map.Entry<BigDecimal, Integer> oldEntry : oldMap.entrySet()) {
            BigDecimal price = oldEntry.getKey();
            if (!newMap.containsKey(price)) {
                updates.add(OrderBookUpdateDTO.builder()
                        .type(OrderBookUpdateDTO.Type.REMOVE)
                        .price(price)
                        .quantity(BigDecimal.valueOf(0)) // 수량 0으로 설정하여 삭제 신호 보냄
                        .build());
            }
        }

        return updates;
    }

    // 호가창을 계산하고 캐시에 저장하는 로직을 별도의 메서드로 분리
    private OrderBookResponseDTO calculateAndCacheOrderBook(Long fundingId) {
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

        // 상한가/하한가 계산
        BigDecimal upperLimitPrice = currentPrice.multiply(BigDecimal.ONE.add(LIMIT_PERCENTAGE))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal lowerLimitPrice = currentPrice.multiply(BigDecimal.ONE.subtract(LIMIT_PERCENTAGE))
                .setScale(2, RoundingMode.HALF_UP);

        // 매수/매도 호가 잔량 집계
        List<OrderVO> activeOrders = orderMapper.findOrdersByFundingId(fundingId);
        List<OrderBookEntryDTO> buyOrders = buildOrderBook(activeOrders, OrderType.BUY, Comparator.reverseOrder());
        List<OrderBookEntryDTO> sellOrders = buildOrderBook(activeOrders, OrderType.SELL, Comparator.naturalOrder());

        OrderBookResponseDTO orderBook = OrderBookResponseDTO.builder()
                .currentPrice(currentPrice)
                .upperLimitPrice(upperLimitPrice)
                .lowerLimitPrice(lowerLimitPrice)
                .buyOrders(buyOrders)
                .sellOrders(sellOrders)
                .timestamp(LocalDateTime.now())
                .build();

        // 계산된 데이터를 캐시에 저장 (예: 30초 TTL 설정)
        String cacheKey = "orderBook:" + fundingId;
        orderBookRedisTemplate.opsForValue().set(cacheKey, orderBook, 30, TimeUnit.SECONDS);

        return orderBook;
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
