package org.bobj.device.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.device.domain.UserDeviceTokenVO;
import org.bobj.device.mapper.UserDeviceTokenMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserDeviceTokenServiceImpl implements UserDeviceTokenService{

    private final UserDeviceTokenMapper userDeviceTokenMapper;

    @Override
    @Transactional
    public void registerToken(Long userId, String deviceToken) {
        UserDeviceTokenVO existingToken = userDeviceTokenMapper.findByDeviceToken(deviceToken);

        if (existingToken != null) {
            // 1. 토큰이 존재하지만, 사용자 ID가 다른 경우
            if (!existingToken.getUserId().equals(userId)) {
                log.warn("기존 디바이스 토큰이 다른 사용자에게 재할당되었습니다. 기존 userId: {} -> 새 userId: {}",
                        existingToken.getUserId(), userId);

                // 기존 토큰의 소유권을 새 사용자에게 넘김
                userDeviceTokenMapper.updateUserId(existingToken.getUserDeviceTokenId(), userId);

                log.info("디바이스 토큰 {}의 소유자가 {}로 변경되었습니다.", deviceToken, userId);
            }
            // 2. 토큰이 존재하고, 사용자 ID도 같은 경우
            else {
                log.debug("디바이스 토큰이 이미 존재하므로 업데이트를 건너뜁니다. userId: {}, token: {}", userId, deviceToken);
            }
        } else {
            // 토큰이 존재하지 않으면 새로운 VO 객체를 만들어 저장
            UserDeviceTokenVO newToken = UserDeviceTokenVO.builder()
                    .userId(userId)
                    .deviceToken(deviceToken)
                    .build();
            userDeviceTokenMapper.insert(newToken);
        }
    }

    @Override
    @Transactional
    public void deleteToken(Long userId, String deviceToken) {
        userDeviceTokenMapper.deleteByUserIdAndDeviceToken(userId, deviceToken);
    }

    @Override
    public String getDeviceTokenByUserId(Long userId) {
        return userDeviceTokenMapper.getDeviceTokenByUserId(userId);
    }

    @Override
    public Map<Long, String> getDeviceTokensByUserIds(List<Long> userIds) {
        List<UserDeviceTokenVO> tokens = userDeviceTokenMapper.getDeviceTokensByUserIds(userIds);
        return tokens.stream()
                .filter(t -> t.getDeviceToken() != null && !t.getDeviceToken().isEmpty())
                .collect(Collectors.toMap(UserDeviceTokenVO::getUserId, UserDeviceTokenVO::getDeviceToken));
    }
}
