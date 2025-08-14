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
    public static class PaymentData {
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

        //  취소 시각(없을 수 있으므로 Long 래퍼)
        @JsonProperty("cancelled_at")     // 포트원은 대부분 이 키를 씀(두 L)
        private Long cancelledAt;


    }



}
