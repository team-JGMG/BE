package org.bobj.trade.service;

import org.bobj.trade.dto.request.TradeHistoryRequestDTO;
import org.bobj.trade.dto.response.FundingTradeHistoryResponseDTO;

public interface TradeHistoryService {
    FundingTradeHistoryResponseDTO getDailyTradeHistory(Long fundingId, TradeHistoryRequestDTO requestDTO);
}
