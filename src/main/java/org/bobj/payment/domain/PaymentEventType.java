package org.bobj.payment.domain;

public enum PaymentEventType {
    PAID, FAILED, CANCELED, PARTIAL_CANCEL;

    public static PaymentEventType fromWebhook(String raw){
        if(raw == null) return FAILED;
        switch (raw.trim().toLowerCase()){
            case "paid": return PAID;
            case "cancelled":       // 포트원 스펠링 케이스
            case "canceled":        return CANCELED; //  DB ENUM에 맞춤(한 L)
            case "partial_cancel":  return PARTIAL_CANCEL;
            default:                return FAILED;
        }
    }

}
