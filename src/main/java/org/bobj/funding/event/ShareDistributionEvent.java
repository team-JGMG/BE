package org.bobj.funding.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ShareDistributionEvent {
    private final Long fundingId;
}
