package org.bobj.notification.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.common.constants.ErrorCode;
import org.bobj.common.exception.CustomException;
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
}
