package org.bobj.common.constants;


import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 401 UNAUTHORIZED
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),

    // 403 FORBIDDEN (권한 없음)
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다."),

    // 404 NOT FOUND
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "N002", "사용자를 찾을 수 없습니다."),

    // 500 ERROR
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S999", "서버 오류가 발생했습니다."),

    // 📦 결제 오류 (Payment)
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "P001", "결제 금액이 일치하지 않습니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "P002", "결제 정보가 존재하지 않습니다."),
    PAYMENT_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "P003", "결제가 완료되지 않았습니다."),
    PAYMENT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "P004", "이미 처리된 결제입니다."),
    PAYMENT_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "P005", "결제 요청 내역이 존재하지 않습니다."),

    // 🔔 알림 오류 (Notification)
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "알림을 찾을 수 없습니다."),

    // 주문 오류
    ORDER_OUT_OF_TRADING_HOURS(HttpStatus.BAD_REQUEST, "OB006", "거래 가능 시간(09:00~15:00)이 아닙니다.");

    private final HttpStatus status;
    private final String code;
    private final String msg;

    ErrorCode(HttpStatus httpStatus, String code, String msg) {
        this.status = httpStatus;
        this.code = code;
        this.msg = msg;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}