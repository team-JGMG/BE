package org.bobj.notification.service;

import org.bobj.notification.dto.response.NotificationResponseDTO;

import java.util.List;

public interface NotificationService {
    List<NotificationResponseDTO> getNotificationsByUserId(Long userId, String readStatus, int page, int size);

    void markNotificationAsRead(Long userId, Long notificationId);

    void markAllNotificationsAsRead(Long userId);

    void registerNotification(Long userId, String title, String body);
}
