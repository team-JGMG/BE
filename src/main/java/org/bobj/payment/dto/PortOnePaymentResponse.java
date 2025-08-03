package org.bobj.payment.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.openid.connect.sdk.SubjectType;
import lombok.Data;

@Data
public class PortOnePaymentResponse {
    private int code;
    private String message;
    private PaymentData response;

    @Data
    public static class PaymentData{
        @JsonProperty("imp_uid")
        private String impUid;

        @JsonProperty("merchant_uid")
        private String merchantUid;

        private int amount;

        private String status;

        @JsonProperty("pay_method")
        private String payMethod;

        @JsonProperty("paid_at")
        private long paidAt;

    }



}
