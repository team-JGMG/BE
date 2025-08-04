package org.bobj.payment.exception;

import org.bobj.common.constants.ErrorCode;
import org.bobj.common.exception.CustomException;
import org.springframework.jdbc.support.SQLErrorCodes;

public class PaymentException extends CustomException {

    public PaymentException(ErrorCode errorCode){
        super(errorCode,errorCode.getStatus().value());
    }

}
