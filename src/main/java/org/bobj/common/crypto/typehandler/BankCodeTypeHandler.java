package org.bobj.common.crypto.typehandler;

import org.bobj.common.crypto.PersonalDataCrypto;

public class BankCodeTypeHandler extends EncryptedStringTypeHandler {

    @Override
    protected PersonalDataCrypto.FieldType getFieldType() {
        return PersonalDataCrypto.FieldType.BANK_CODE;
    }
}