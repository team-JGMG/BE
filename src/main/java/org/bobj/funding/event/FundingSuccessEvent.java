package org.bobj.funding.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FundingSuccessEvent {
    private final Long fundingId;
}
