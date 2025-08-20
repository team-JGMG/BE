package org.bobj.orderbook.dto.response;

import lombok.Builder;
import lombok.Data;
import org.bobj.orderbook.dto.OrderBookEntryDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderBookUpdateDTO {
    /**
     * 업데이트 유형을 정의하는 열거형 (Enum).
     * <p>
     * - FULL: 전체 호가창 업데이트
     * - ADD: 새로운 호가 가격 추가
     * - UPDATE: 기존 호가 수량 변경
     * - REMOVE: 호가 가격 삭제 (수량 0)
     * - PRICE_UPDATE: 현재가만 변경
     */
    public enum Type {
        FULL, ADD, UPDATE, REMOVE, PRICE_UPDATE
    }
    // 변경 유형
    private Type type;

    // 변경된 호가의 가격
    private BigDecimal price;

    // 변경된 수량
    private BigDecimal quantity;

    // 이 업데이트가 매수(BUY)인지 매도(SELL)인지 구분
    private String orderType;

    // 변경이 발생한 시점의 타임스탬프
    private LocalDateTime timestamp;

    // 전체 호가창 업데이트 시 사용 (FULL 타입)
    private List<OrderBookEntryDTO> buyOrders;
    private List<OrderBookEntryDTO> sellOrders;

    // 가격 정보 업데이트 시 사용 (PRICE_UPDATE 타입)
    private BigDecimal currentPrice;
    private BigDecimal upperLimitPrice;
    private BigDecimal lowerLimitPrice;
}
