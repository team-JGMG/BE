package org.bobj.trade.mapper;

import org.bobj.trade.domain.TradeVO;

public interface TradeMapper {

    // 체결 내역을 저장
    void insert(TradeVO tradeVO);

}
