package org.bobj.point.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.payment.client.PortOneClient;
import org.bobj.payment.dto.PortOneWebhookRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointWebhookService {

    private final PortOneClient portOneClient;
    private final PointChargeRequestService pointChargeRequestService;

    public void process(PortOneWebhookRequest request) {
        // TODO: 결제 검증 및 포인트 지급 로직

    }

}
