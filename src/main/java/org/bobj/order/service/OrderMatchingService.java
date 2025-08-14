package org.bobj.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.funding.service.FundingService;
import org.bobj.notification.service.NotificationService;
import org.bobj.order.domain.OrderVO;
import org.bobj.order.domain.OrderType;
import org.bobj.order.mapper.OrderMapper;
import org.bobj.orderbook.service.OrderBookService;
import org.bobj.point.domain.PointVO;
import org.bobj.point.service.PointService;
import org.bobj.share.domain.ShareVO;
import org.bobj.share.mapper.ShareMapper;
import org.bobj.trade.domain.TradeVO;
import org.bobj.trade.mapper.TradeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class OrderMatchingService {

    private final OrderMapper orderMapper;
    private final TradeMapper tradeMapper;
    private final ShareMapper shareMapper;
    private final PointService pointService;

    private final OrderBookService orderBookService;
    private final FundingService fundingService;
    private final NotificationService notificationService;


    @Transactional
    public int processOrderMatching(OrderVO newOrder) {

        // 1. 매칭될 반대편 주문 조회
        // 신규 주문이 BUY 이면 SELL 주문을, SELL 이면 BUY 주문을 찾는다.
        String oppositeOrderType = String.valueOf(newOrder.getOrderType() == OrderType.BUY
                ? OrderType.SELL
                : OrderType.BUY);

        // 매칭 가능한 주문들을 조회
        // 2. 대기 상태이면서, 같은 종목에 대한 반대 주문 리스트 가져오기
        List<OrderVO> matchingOrders = orderMapper.findMatchingOrders(
                newOrder.getFundingId(),
                newOrder.getOrderPricePerShare(),
                oppositeOrderType,
                String.valueOf(newOrder.getOrderType())
        );

        log.debug("🔍 매칭 대상 주문 수: {}", matchingOrders.size());

        // 3. 매칭 조건이 되는 주문과 체결 시도
        int remainingNewOrderCount = newOrder.getOrderShareCount(); // 신규 주문의 남은 수량

        // 체결 내역 리스트
        List<TradeVO> trades = new ArrayList<>();

        for (OrderVO matchedOrder : matchingOrders) {
            if (remainingNewOrderCount <= 0) {
                break;
            }

            // 2-1. 체결 가능한 수량 계산
            int tradeCount = Math.min(remainingNewOrderCount, matchedOrder.getRemainingShareCount());

            // 2-2. 체결 가격 결정
            BigDecimal actualTradePrice = matchedOrder.getOrderPricePerShare(); // 상대방 주문 가격으로 체결

            // 2-3. TRADES 테이블에 체결 내역 기록
            Long buyOrderId = newOrder.getOrderType() == OrderType.BUY
                    ? newOrder.getOrderId()
                    : matchedOrder.getOrderId();

            Long sellOrderId = newOrder.getOrderType() == OrderType.SELL
                    ? newOrder.getOrderId()
                    : matchedOrder.getOrderId();

            Long buyerUserId = newOrder.getOrderType() == OrderType.BUY
                    ? newOrder.getUserId()
                    : matchedOrder.getUserId();

            Long sellerUserId = newOrder.getOrderType() == OrderType.SELL
                    ? newOrder.getUserId()
                    : matchedOrder.getUserId();

            TradeVO tradeVO = TradeVO.builder()
                    .buyOrderId(buyOrderId)
                    .sellOrderId(sellOrderId)
                    .buyerUserId(buyerUserId)
                    .sellerUserId(sellerUserId)
                    .tradeCount(tradeCount)
                    .tradePricePerShare(actualTradePrice)
                    .build();

            tradeMapper.insert(tradeVO);

            trades.add(tradeVO);

            // 2-4. OrderBook 업데이트 (신규 주문 및 매칭된 상대 주문)
            // 신규 주문의 남은 수량 감소
            remainingNewOrderCount -= tradeCount;
            // 매칭된 상대 주문의 남은 수량 감소
            int newMatchedOrderRemainingCount = matchedOrder.getRemainingShareCount() - tradeCount;

            // 신규 주문 상태 업데이트
            String newOrderStatus = (remainingNewOrderCount == 0) ? "FULLY_FILLED" : "PARTIALLY_FILLED";
            orderMapper.updateOrderBookStatusAndRemainingCount(
                    newOrder.getOrderId(),
                    newOrderStatus,
                    remainingNewOrderCount
            );

            // 매칭된 상대 주문 상태 업데이트
            String matchedOrderStatus = (newMatchedOrderRemainingCount == 0) ? "FULLY_FILLED" : "PARTIALLY_FILLED";
            orderMapper.updateOrderBookStatusAndRemainingCount(
                    matchedOrder.getOrderId(),
                    matchedOrderStatus,
                    newMatchedOrderRemainingCount
            );

            // 2-5. 사용자 자산(주식 수량 및 포인트) 업데이트
            //  매수자 포인트 업데이트
            processBuyTradeAssets(buyerUserId, newOrder.getFundingId(), tradeCount, actualTradePrice);
            // 매도자 포인트 업데이트
            processSellTradeAssets(sellerUserId, newOrder.getFundingId(), tradeCount, actualTradePrice);
        }

        // 알림 전송 단계
        if (!trades.isEmpty()) {

            String propertyTitle = fundingService.getPropertyTitleByFundingId(newOrder.getFundingId());

            // 1. 매수자에게 전체 합산 알림 1번
            Long buyerUserId = trades.get(0).getBuyerUserId(); // 모든 체결의 매수자는 동일
            int totalTradeCount = trades.stream().mapToInt(TradeVO::getTradeCount).sum();
            BigDecimal lastTradePrice = trades.get(trades.size() - 1).getTradePricePerShare();

            String title = propertyTitle + " 거래가 체결되었어요!";
            String buyerBody = totalTradeCount + "주가 " + lastTradePrice + "원에 체결되었습니다.";

            notificationService.sendNotificationAndSave(buyerUserId, title, buyerBody);
            // 2. 매도자별로 각각 알림 1번
            trades.stream()
                    .collect(Collectors.groupingBy(TradeVO::getSellerUserId))
                    .forEach((sellerUserId, sellerTrades) -> {
                        int sellerTotalCount = sellerTrades.stream().mapToInt(TradeVO::getTradeCount).sum();
                        BigDecimal sellerLastPrice = sellerTrades.get(sellerTrades.size() - 1).getTradePricePerShare();

                        String sellerBody = sellerTotalCount + "주가 " + sellerLastPrice + "원에 체결되었습니다.";
                        notificationService.sendNotificationAndSave(sellerUserId, title, sellerBody);;
                    });
        }

        return remainingNewOrderCount;
    }

    // 매수자 포인트 업데이트
    private void processBuyTradeAssets(Long userId, Long fundingId, int tradedCount, BigDecimal actualTradePrice) {
        // 1. 사용자 포인트 감소
        BigDecimal totalTradeCost = actualTradePrice.multiply(new BigDecimal(tradedCount));

        // 해당 user의 현재 PointVO를 조회 (락 필요)
        PointVO userPoint = pointService.findByUserIdForUpdate(userId); // findByIdForUpdate, findByUserIdForUpdate 등 락 적용된 메서드 필요

        if (userPoint.getAmount().compareTo(totalTradeCost) < 0) {
            throw new IllegalStateException("매수자의 포인트가 부족합니다.");
        }

        userPoint.setAmount(userPoint.getAmount().subtract(totalTradeCost));
        pointService.updatePoint(userPoint);

        // 2. SHARES 테이블 업데이트
        // 해당 사용자의 해당 펀딩에 대한 현재 보유 주식 정보 조회
        ShareVO existingShare = shareMapper.findUserShareByFundingIdForUpdate(userId, fundingId);

        if (existingShare == null) {
            // 해당 종목을 처음 매수하는 경우: 새로운 SHARES 레코드 생성 (INSERT)
            ShareVO newShare = ShareVO.builder()
                    .userId(userId)
                    .fundingId(fundingId)
                    .shareCount(tradedCount)
                    .averageAmount(actualTradePrice)
                    .build();

            shareMapper.insert(newShare);
        } else {
            // 이미 보유하고 있는 경우: 수량 증가 및 평균 단가 재계산 (UPDATE)
            int newShareCount = existingShare.getShareCount() + tradedCount;

            // 기존 총 매입 금액 = 기존 평균 단가 * 기존 보유 수량
            BigDecimal existingTotalAmount = existingShare.getAverageAmount().multiply(new BigDecimal(existingShare.getShareCount()));
            // 신규 매입 금액 = 현재 체결 가격 * 체결 수량
            BigDecimal newTradeAmount = actualTradePrice.multiply(new BigDecimal(tradedCount));

            // 새로운 총 매입 금액 = 기존 총 매입 금액 + 신규 매입 금액
            BigDecimal combinedTotalAmount = existingTotalAmount.add(newTradeAmount);

            // 새로운 평균 매입 단가 = 새로운 총 매입 금액 / 새로운 총 주식 수량
            // DECIMAL(18, 4)에 맞춰 소수점 4자리까지 반올림
            BigDecimal newAverageAmount = combinedTotalAmount.divide(new BigDecimal(newShareCount), 4, BigDecimal.ROUND_HALF_UP);

            // SHARES 테이블 업데이트
            shareMapper.update(existingShare.getShareId(), newShareCount, newAverageAmount);
        }
    }

    // 매도자 포인트 업데이트
    private void processSellTradeAssets(Long userId, Long fundingId, int tradedCount, BigDecimal actualTradePrice) {
        // 1. 사용자 포인트 증가
        BigDecimal totalTradeRevenue = actualTradePrice.multiply(new BigDecimal(tradedCount));

        // 사용자 포인트 조회
        PointVO userPoint = pointService.findByUserIdForUpdate(userId);

        userPoint.setAmount(userPoint.getAmount().add(totalTradeRevenue));
        pointService.updatePoint(userPoint);

        // 2. SHARES 테이블 업데이트
        // 사용자의 해당 펀딩에 대한 현재 보유 주식 정보 조회
        ShareVO existingShare = shareMapper.findUserShareByFundingIdForUpdate(userId, fundingId); // 락 적용된 조회 메서드

        // 주식을 보유하고 있는지 확인 (유효성 검사는 주문 접수 시점에서 이루어져야 하지만, 안전을 위해 다시 확인)
        if (existingShare == null || existingShare.getShareCount() < tradedCount) {
            throw new IllegalStateException("매도자의 주식 보유량이 부족하거나, 존재하지 않는 주식입니다.");
        }

        int newShareCount = existingShare.getShareCount() - tradedCount;

        if (newShareCount == 0) {
            // 모든 주식을 매도하여 보유량이 0이 된 경우: SHARES 레코드 삭제
            shareMapper.delete(existingShare.getShareId());
        } else {
            // 일부 주식을 매도한 경우: 수량만 감소 (평균 단가는 매도 시 변하지 않음)
            shareMapper.update(existingShare.getShareId(), newShareCount, existingShare.getAverageAmount());
        }
    }

}
