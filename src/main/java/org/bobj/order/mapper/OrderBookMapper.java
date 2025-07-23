package org.bobj.order.mapper;

import org.apache.ibatis.annotations.Param;
import org.bobj.order.domain.OrderBookVO;

import java.util.List;

public interface OrderBookMapper {

    void create(OrderBookVO orderBookVO);

    OrderBookVO get(Long orderId);

    List<OrderBookVO> getOrderHistoryByUserId( @Param("userId") Long userId,
                                               @Param("orderType") String orderType);
    OrderBookVO getForUpdate(Long orderId);

    void cancelOrder(Long orderId);
}
