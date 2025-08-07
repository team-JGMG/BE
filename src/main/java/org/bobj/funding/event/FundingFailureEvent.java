package org.bobj.funding.event;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class FundingFailureEvent {
    private final Long fundingId;
}
