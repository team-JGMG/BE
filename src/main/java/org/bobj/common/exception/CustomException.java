package org.bobj.common.exception;

import lombok.Getter;
import org.bobj.common.constants.ErrorCode;

@Getter
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;
    private final int status;

    public CustomException(ErrorCode errorCode, int status) {
        super(errorCode.getMsg());
        this.errorCode = errorCode;
        this.status = status;
    }

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.errorCode = errorCode;
        this.status = errorCode.getStatus().value();
    }

}
