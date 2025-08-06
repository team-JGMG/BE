package org.bobj.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationVO {
    Long notificationId;
    Long userId;
    private String title;
    private String body;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
