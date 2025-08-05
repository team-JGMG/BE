package org.bobj.device.mapper;

import org.apache.ibatis.annotations.Param;
import org.bobj.device.domain.UserDeviceTokenVO;

import java.util.List;

public interface UserDeviceTokenMapper {
    // 디바이스 토큰이 이미 존재하는지 확인
    UserDeviceTokenVO findByDeviceToken(String deviceToken);

    // 새로운 디바이스 토큰 저장
    void insert(UserDeviceTokenVO userDeviceToken);

    // 특정 사용자의 모든 디바이스 토큰 조회
    List<UserDeviceTokenVO> findByUserId(@Param("userId") Long userId);

    // 사용자 ID와 토큰으로 레코드 삭제
    void deleteByUserIdAndDeviceToken(@Param("userId") Long userId, @Param("deviceToken") String deviceToken);

    // 기존 디바이스 토큰 사용자 ID 업데이트
    void updateUserId(@Param("userDeviceTokenId") Long userDeviceTokenId, @Param("userId") Long userId);

}
