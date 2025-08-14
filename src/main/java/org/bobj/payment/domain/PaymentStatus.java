package org.bobj.payment.domain;

public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    EXPIRED,
    CANCELLED;

    /*
    * PortOne Webhook 상태 문자열을 내부 상태로 매핑
    * 안전을 위해 모르는 값은 FAILED 로 처리
    *  */

    public static PaymentStatus fromWebhook(String raw){
        if(raw == null) return FAILED;
        String s = raw.trim().toLowerCase();
        switch (s) {
            case "paid":            return SUCCESS;
            case "partial_cancel":
            case "cancelled":
            case "canceled":        return CANCELLED;
            case "failed":          return FAILED;
            case "expired":         return EXPIRED;
            case "ready":
            case "pay_pending":     return PENDING;   // 일부 PG에서 대기 상태
            default:                return FAILED;    // 알 수 없는 값은 보수적으로 실패 처리
        }
    }
    /** 더 이상 바뀌지 않는 최종 상태 여부 */
    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == EXPIRED || this == CANCELLED;
    }

}
