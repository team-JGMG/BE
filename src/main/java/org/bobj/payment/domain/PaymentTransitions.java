package org.bobj.payment.domain;

public final class PaymentTransitions {

    private PaymentTransitions() {
    }

    /*
     * 허용 전이 :
     * PENDING -> SUCCESS / FAILED / EXPIRED
     * SUCCESS -> CANCELLED
     * 동일 상태 유지 허용
     */
    public static boolean allowed(PaymentStatus from, PaymentStatus to) {
        if (from == null || from == to)
            return true;
        switch (from) {
            case PENDING:
                return to == PaymentStatus.SUCCESS
                    || to == PaymentStatus.FAILED
                    || to == PaymentStatus.EXPIRED;
            case SUCCESS:
                return to == PaymentStatus.CANCELLED;
            default:
                return false; // FAILED/EXPIRED/CANCELLED 에서 되돌리기 금지
        }
    }
}
