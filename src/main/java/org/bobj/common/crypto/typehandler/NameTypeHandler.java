package org.bobj.common.crypto.typehandler;

import org.bobj.common.crypto.PersonalDataCrypto;

public class NameTypeHandler extends EncryptedStringTypeHandler {

    @Override
    protected PersonalDataCrypto.FieldType getFieldType() {
        return PersonalDataCrypto.FieldType.NAME;
    }
}