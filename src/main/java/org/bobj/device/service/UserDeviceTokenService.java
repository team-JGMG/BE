package org.bobj.device.service;

import org.bobj.device.domain.UserDeviceTokenVO;

import java.util.List;
import java.util.Map;

public interface UserDeviceTokenService {
    void registerToken(Long userId, String deviceToken);

    void deleteToken(Long userId, String deviceToken);

    String getDeviceTokenByUserId(Long userId);

    // 여러 사용자 ID에 대한 디바이스 토큰을 한 번에 조회합니다.
    Map<Long, String> getDeviceTokensByUserIds(List<Long> userIds);
}
