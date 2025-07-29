package org.bobj.common.crypto.typehandler;

import org.bobj.common.crypto.PersonalDataCrypto;

public class SsnTypeHandler extends EncryptedStringTypeHandler {

    @Override
    protected PersonalDataCrypto.FieldType getFieldType() {
        return PersonalDataCrypto.FieldType.SSN;
    }
}