package org.bobj.order.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OrderPlacedEvent {
    private final Long fundingId;
    private final Long orderId;
}