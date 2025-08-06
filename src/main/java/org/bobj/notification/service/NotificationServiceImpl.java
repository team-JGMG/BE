package org.bobj.notification.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.common.constants.ErrorCode;
import org.bobj.common.exception.CustomException;
import org.bobj.device.service.UserDeviceTokenService;
import org.bobj.fcm.dto.request.FcmRequestDto;
import org.bobj.fcm.service.FcmService;
import org.bobj.notification.domain.NotificationVO;
import org.bobj.notification.dto.response.NotificationResponseDTO;
import org.bobj.notification.mapper.NotificationMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService{

    private final NotificationMapper notificationMapper;
    private final FcmService fcmService;
    private final UserDeviceTokenService userDeviceTokenService;

    @Override
    public List<NotificationResponseDTO> getNotificationsByUserId(Long userId, String readStatus, int page, int size) {
        int offset = page * size;
        List<NotificationVO> notifications = notificationMapper.findNotificationsByUserId(userId, readStatus, offset, size);

        return notifications.stream()
                .map(NotificationResponseDTO::of)
                .collect(Collectors.toList());
    }

    //특정 알림 읽음 처리
    @Override
    public void markNotificationAsRead(Long userId, Long notificationId) {
        NotificationVO notification = notificationMapper.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
        }

        if (!notification.getIsRead()) {
            notificationMapper.markAsRead(notificationId);
        }
    }

    //모든 알림 읽음 처리
    @Override
    public void markAllNotificationsAsRead(Long userId) {
        notificationMapper.markAllAsReadByUserId(userId);
    }

    @Override
    public void registerNotification(Long userId, String title, String body) {
        NotificationVO notificationVO = NotificationVO.builder()
                .userId(userId)
                .title(title)
                .body(body)
                .build();

        notificationMapper.registerNotification(notificationVO);
    }

    @Override
    public void sendNotificationAndSave(Long userId, String title, String body) {
        try {
            String deviceToken = userDeviceTokenService.getDeviceTokenByUserId(userId);
            log.info("사용자(ID: {})의 디바이스 토큰: {}", userId, deviceToken);

            if (deviceToken != null) {
                FcmRequestDto fcmDto = FcmRequestDto.builder()
                        .deviceToken(deviceToken)
                        .title(title)
                        .body(body)
                        .build();
                fcmService.sendMessageTo(fcmDto);
            }
        } catch (Exception e) {
            log.error("사용자(ID: {}) FCM 알림 발송 실패", userId, e);
        } finally {
            // FCM 발송 성공/실패와 관계없이 DB에 알림을 저장
            registerNotification(userId, title, body);
        }
    }
}
