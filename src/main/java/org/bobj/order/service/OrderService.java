package org.bobj.order.service;

import org.bobj.order.dto.request.OrderRequestDTO;
import org.bobj.order.dto.response.OrderResponseDTO;

import java.util.List;


public interface OrderService {

    OrderResponseDTO getOrderById(Long orderId);

    OrderResponseDTO placeOrder(OrderRequestDTO orderRequestDTO);

    List<OrderResponseDTO> getOrderHistoryByUserId(Long userId, String orderType);

    Long cancelOrder(Long orderId);

}

