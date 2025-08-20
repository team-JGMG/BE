package org.bobj.orderbook.service;

import org.bobj.orderbook.dto.response.OrderBookResponseDTO;
import org.bobj.orderbook.dto.response.OrderBookUpdateDTO;

import java.util.List;

public interface OrderBookService {
    OrderBookResponseDTO getOrderBookByFundingId(Long fundingId);
    List<OrderBookUpdateDTO> updateOrderBookAndSendUpdates(Long fundingId);
}

