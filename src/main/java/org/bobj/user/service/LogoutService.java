package org.bobj.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.device.service.UserDeviceTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutService {

    private final UserService userService;
    private final UserDeviceTokenService userDeviceTokenService;

    @Transactional
    public void processLogout(Long userId, String email,  String deviceToken) {

        // 1. 리프레시 토큰 DB에서 제거
        userService.removeRefreshToken(email);
        log.info("리프레시 토큰 제거 완료: email={}", email);

        // 2. 디바이스 토큰 DB에서 제거
        if (userId != null) {
            userDeviceTokenService.deleteToken(userId, deviceToken);
            log.info("디바이스 토큰 제거 완료: userId={}", userId);
        }

        log.info("로그아웃 서비스 완료: email={}", email);
    }
}
