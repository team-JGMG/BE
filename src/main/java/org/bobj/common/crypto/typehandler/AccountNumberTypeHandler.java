package org.bobj.common.crypto.typehandler;

import org.bobj.common.crypto.PersonalDataCrypto;

/**
 * 계좌번호 필드 암호화 TypeHandler
 * 최고 보안 등급 적용
 */
public class AccountNumberTypeHandler extends EncryptedStringTypeHandler {

    @Override
    protected PersonalDataCrypto.FieldType getFieldType() {
        return PersonalDataCrypto.FieldType.ACCOUNT_NUMBER;
    }
}
