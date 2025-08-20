package org.bobj.orderbook.service;

import org.bobj.orderbook.dto.response.OrderBookResponseDTO;

public interface OrderBookService {
    OrderBookResponseDTO getOrderBookByFundingId(Long fundingId);
    void evictOrderBookCache(Long fundingId);
}

