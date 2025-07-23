package org.bobj.common.exception;



import javax.servlet.http.HttpServletRequest;
import org.bobj.common.constants.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {



    // 일반 예외
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest requeset) {
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
        ErrorResponse errorResponse = ErrorResponse.from(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // 그 외 모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }


}