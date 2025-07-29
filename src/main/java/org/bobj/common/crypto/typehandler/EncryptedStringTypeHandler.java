package org.bobj.common.crypto.typehandler;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.bobj.common.crypto.PersonalDataCrypto;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 개인정보 자동 암호화/복호화 TypeHandler 기본 클래스
 * MyBatis에서 DB 저장/조회 시 자동으로 암호화/복호화 수행
 */
@Slf4j
public abstract class EncryptedStringTypeHandler extends BaseTypeHandler<String> {

    // Spring DI 제거 - 정적 메서드 사용

    /**
     * 하위 클래스에서 필드 타입을 반환하도록 구현
     */
    protected abstract PersonalDataCrypto.FieldType getFieldType();

    /**
     * DB에 저장할 때: 평문 → 암호문
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        try {
            String encryptedValue = PersonalDataCrypto.encryptStatic(parameter, getFieldType());
            ps.setString(i, encryptedValue);
            log.debug("개인정보 암호화 완료: fieldType={}", getFieldType());
        } catch (Exception e) {
            log.error("개인정보 암호화 실패: fieldType={}, error={}", getFieldType(), e.getMessage());
            throw new SQLException("개인정보 암호화에 실패했습니다.", e);
        }
    }

    /**
     * DB에서 조회할 때: 암호문 → 평문
     */
    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String encryptedValue = rs.getString(columnName);
        return decryptValue(encryptedValue);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String encryptedValue = rs.getString(columnIndex);
        return decryptValue(encryptedValue);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String encryptedValue = cs.getString(columnIndex);
        return decryptValue(encryptedValue);
    }

    /**
     * 복호화 공통 로직
     */
    private String decryptValue(String encryptedValue) throws SQLException {
        if (encryptedValue == null) {
            return null;
        }

        try {
            String decryptedValue = PersonalDataCrypto.decryptStatic(encryptedValue, getFieldType());
            log.debug("개인정보 복호화 완료: fieldType={}", getFieldType());
            return decryptedValue;
        } catch (Exception e) {
            log.error("개인정보 복호화 실패: fieldType={}, encryptedValue={}, error={}",
                    getFieldType(), encryptedValue, e.getMessage());
            throw new SQLException("개인정보 복호화에 실패했습니다.", e);
        }
    }
}