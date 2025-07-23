package org.bobj.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.order.domain.OrderBookVO;
import org.bobj.order.domain.OrderStatus;
import org.bobj.order.domain.OrderType;
import org.bobj.order.dto.request.OrderBookRequestDTO;
import org.bobj.order.dto.response.OrderBookResponseDTO;
import org.bobj.order.mapper.OrderBookMapper;
import org.bobj.share.mapper.ShareMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;


@Log4j2
@Service
@RequiredArgsConstructor
public class OrderBookServiceImpl implements OrderBookService {

    final private OrderBookMapper orderBookMapper;
    final private ShareMapper shareMapper;
//    final private TradeMapper tradeMapper;

    @Override
    public OrderBookResponseDTO getOrderById(Long orderId) {
        OrderBookResponseDTO board =OrderBookResponseDTO.of(orderBookMapper.get(orderId));

        return Optional.ofNullable(board).orElseThrow(NoSuchElementException::new);
    }


    @Transactional
    @Override
    public OrderBookResponseDTO placeOrder(OrderBookRequestDTO orderBookRequestDTO) {

        OrderBookVO orderBookVO = orderBookRequestDTO.toVo();

        // 1. 펀딩 상태 확인
//        String fundingStatus = mapper.findFundingStatusByFundingId(orderBookVO.getFundingId());
//        if (!"ENDED".equals(fundingStatus)) {
//            throw new IllegalStateException("펀딩이 종료된 후에만 거래가 가능합니다.");
//        }

        // 2. 매도인일 경우 주식 보유 수량 확인
        if ("SELL".equalsIgnoreCase(String.valueOf(orderBookVO.getOrderType()))) {
            Integer userShareCount = shareMapper.findUserShareCount(orderBookVO.getUserId(), orderBookVO.getFundingId());

            // 보유 수량이 없는 경우
            if (userShareCount == null || userShareCount == 0) {
                throw new IllegalStateException("해당 종목을 보유하고 있지 않습니다.");
            }

            // 수량은 있으나 매도 수량보다 적은 경우
            if (userShareCount < orderBookVO.getOrderShareCount()) {
                throw new IllegalArgumentException("보유 주식 수량보다 많은 수량을 매도할 수 없습니다.");
            }
        }

        // 3. 매수인일 경우, 총 발행 주식 수보다 많은 수량을 주문하는 경우 체크
//        if ("BUY".equals(orderBookVO.getOrderType())) {
//            int totalIssuedShares = mapper.findTotalIssuedShares(orderBookVO.getFundingId());
//            int totalBuyOrderCount = mapper.findTotalBuyOrderCount(orderBookVO.getFundingId());
//            if (totalBuyOrderCount + orderBookVO.getOrderCount() > totalIssuedShares) {
//                throw new IllegalArgumentException("총 발행 주식 수를 초과하는 수량은 매수할 수 없습니다.");
//            }
//        }
//
//        // 4. 중복 주문 방지
//        boolean alreadyTraded = mapper.isOrderAlreadyTraded(orderBookVO);
//        if (alreadyTraded) {
//            throw new IllegalStateException("이미 체결된 주문입니다.");
//        }


        orderBookMapper.create(orderBookVO);

        return getOrderById(orderBookVO.getOrderId());
    }

    @Override
    public List<OrderBookResponseDTO> getOrderHistoryByUserId(Long userId, String orderTypeStr) {
        OrderType orderType = null;

        if (orderTypeStr != null && !orderTypeStr.trim().isEmpty()) {
            try {
                orderType = OrderType.valueOf(orderTypeStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("유효하지 않은 주문 타입입니다. (BUY 또는 SELL만 가능)");
            }
        }

        List<OrderBookVO> orderBooks = orderBookMapper.getOrderHistoryByUserId(userId, orderType != null ? orderType.name() : null);
        return orderBooks.stream().map(OrderBookResponseDTO::of).toList();
    }

    @Transactional
    @Override
    public void cancelOrder(Long orderId) {
        OrderBookVO orderBook = orderBookMapper.getForUpdate(orderId);

        if (orderBook.getStatus() == OrderStatus.MATCHED) {
            throw new IllegalStateException("이미 체결된 주문은 취소할 수 없습니다.");
        }

        if (orderBook.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 주문입니다.");
        }

        orderBookMapper.cancelOrder(orderId);
    }
}
