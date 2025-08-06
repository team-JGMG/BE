package org.bobj.device.service;

public interface UserDeviceTokenService {
    void registerToken(Long userId, String deviceToken);

    void deleteToken(Long userId, String deviceToken);

    String getDeviceTokenByUserId(Long userId);
}
