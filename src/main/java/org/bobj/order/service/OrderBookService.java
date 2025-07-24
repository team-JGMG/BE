package org.bobj.order.service;

import org.bobj.order.dto.request.OrderBookRequestDTO;
import org.bobj.order.dto.response.OrderBookResponseDTO;

import java.util.List;


public interface OrderBookService {

    OrderBookResponseDTO getOrderById(Long orderId);

    OrderBookResponseDTO placeOrder(OrderBookRequestDTO orderBookRequestDTO);

    List<OrderBookResponseDTO> getOrderHistoryByUserId(Long userId, String orderType);

    void cancelOrder(Long orderId);
}
