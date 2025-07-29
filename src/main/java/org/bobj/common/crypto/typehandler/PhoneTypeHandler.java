package org.bobj.common.crypto.typehandler;

import org.bobj.common.crypto.PersonalDataCrypto;

public class PhoneTypeHandler extends EncryptedStringTypeHandler {

    @Override
    protected PersonalDataCrypto.FieldType getFieldType() {
        return PersonalDataCrypto.FieldType.PHONE;
    }
}