package org.bobj.common.exception;



import javax.servlet.http.HttpServletRequest;
import org.bobj.common.constants.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.bobj.user.util.CookieUtil;



@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final CookieUtil cookieUtil = new CookieUtil();


    // 일반 예외
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest requeset) {
        cookieUtil.logAllCookies(requeset);
        log.error("IllegalArgumentException 발생: {}, URI: {}", ex.getMessage(), requeset.getRequestURI(), ex);
        ErrorResponse errorResponse = ErrorResponse.from(HttpStatus.BAD_REQUEST, ex.getMessage(), requeset.getRequestURI());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    // IllegalStateException 처리 (400 Bad Request 또는 409 Conflict 등)
    // 이 예외는 주로 비즈니스 로직의 '상태'가 유효하지 않을 때 발생합니다.
    // OrderBookServiceImpl에서 던지는 IllegalStateException은 ErrorCode를 직접 사용하도록 변경할 것이므로,
    // 여기서는 일반적인 IllegalStateException을 처리하는 용도로 남겨두거나,
    // BusinessException을 도입하여 더 세분화할 수 있습니다.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
          // 기본적으로 BAD_REQUEST로 처리하되, 특정 메시지에 따라 CONFLICT 등으로 변경할 수 있습니다.
        // 예: if (ex.getMessage().contains("이미 체결된 주문입니다.")) return ResponseEntity.status(HttpStatus.CONFLICT).body(...);
        cookieUtil.logAllCookies(request);
        ErrorResponse errorResponse = ErrorResponse.from(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // 그 외 모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        // 실제 예외 정보를 로그에 출력
        cookieUtil.logAllCookies(request);
        log.error("예상치 못한 서버 오류 발생: {}, URI: {}", ex.getMessage(), request.getRequestURI(), ex);
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }


}