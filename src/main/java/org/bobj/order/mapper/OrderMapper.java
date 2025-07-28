package org.bobj.order.mapper;

import org.apache.ibatis.annotations.Param;
import org.bobj.order.domain.OrderVO;

import java.math.BigDecimal;
import java.util.List;

public interface OrderMapper {

    void create(OrderVO orderVO);

    OrderVO get(Long orderId);

    List<OrderVO> getOrderHistoryByUserId(@Param("userId") Long userId,
                                          @Param("orderType") String orderType);
    OrderVO getForUpdate(Long orderId);

    void cancelOrder(Long orderId);

    void updateOrderBookStatusAndRemainingCount(  //주문이 체결되거나 부분 체결될 때, 해당 주문의 남은 수량과 상태를 업데이트
            @Param("orderId") Long orderId,
            @Param("status") String status,
            @Param("remainingShareCount") int remainingShareCount
    );

    List<OrderVO> findMatchingOrders(
            @Param("fundingId") Long fundingId,
            @Param("newOrderPrice") BigDecimal newOrderPrice,
            @Param("oppositeOrderType") String oppositeOrderType,
            @Param("newOrderType") String newOrderType
    );

    List<OrderVO> findOrdersByFundingId(Long fundingId);
}
