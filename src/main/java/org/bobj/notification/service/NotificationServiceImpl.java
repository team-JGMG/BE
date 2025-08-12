package org.bobj.notification.service;


import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.common.constants.ErrorCode;
import org.bobj.common.exception.CustomException;
import org.bobj.device.domain.UserDeviceTokenVO;
import org.bobj.device.service.UserDeviceTokenService;
import org.bobj.fcm.dto.request.FcmRequestDto;
import org.bobj.fcm.service.FcmService;
import org.bobj.notification.domain.NotificationVO;
import org.bobj.notification.dto.response.NotificationResponseDTO;
import org.bobj.notification.mapper.NotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
        NotificationVO notification = Optional.ofNullable(notificationMapper.findById(notificationId))
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

    @Override
    public void sendBatchNotificationsAndSave(List<Long> userIds, String title, String body) {
        // 1. 모든 사용자의 디바이스 토큰 조회
        Map<Long, String> userTokenMap = userDeviceTokenService.getDeviceTokensByUserIds(userIds);

        // 2. 토큰이 있는 사용자들만 필터링
        List<Long> validUserIds = userIds.stream()
                .filter(userTokenMap::containsKey)
                .toList();

        if (validUserIds.isEmpty()) {
            log.warn("유효한 디바이스 토큰을 가진 사용자가 없습니다.");
            // 토큰이 없어도 DB에는 알림을 저장
            // registerBatchNotifications(userIds, title, body);
            return;
        }

        // 3. FCM 토큰 리스트 생성
        List<String> fcmTokens = validUserIds.stream()
                .map(userTokenMap::get)
                .toList();


        // 4. FCM 발송 (예외 처리)
        try {
            fcmService.sendMulticast(fcmTokens, title, body);
        } catch (IOException e) {
            log.error("FCM 발송 중 IOException 발생", e);
        }

        // 5. 모든 사용자(FCM 성공/실패 무관)에게 DB 알림 저장
        registerBatchNotifications(userIds, title, body);

        log.info("배치 알림 발송 완료 - 처리된 사용자 수: {}", userIds.size());
    }


    //알림 Notifications 테이블에 저장
    private void registerBatchNotifications(List<Long> userIds, String title, String body) {
        List<NotificationVO> notifications = userIds.stream()
                .map(userId -> NotificationVO.builder()
                        .userId(userId)
                        .title(title)
                        .body(body)
                        .build())
                .collect(Collectors.toList());

        notificationMapper.insertBatchNotifications(notifications);
        log.info("배치 알림 DB 저장 완료 - 저장된 알림 수: {}", notifications.size());
    }

}
