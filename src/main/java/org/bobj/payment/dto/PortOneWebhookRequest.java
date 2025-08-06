package org.bobj.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PortOneWebhookRequest {
    private String imp_uid;
    private String merchant_uid;
    private String status;
}
