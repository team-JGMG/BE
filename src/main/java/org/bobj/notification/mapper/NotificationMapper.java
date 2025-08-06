package org.bobj.notification.mapper;

import org.apache.ibatis.annotations.Param;
import org.bobj.notification.domain.NotificationVO;

import java.util.List;
import java.util.Optional;

public interface NotificationMapper {

    //알림등록
    void registerNotification(NotificationVO notification);

    //ID로 특정 알림을 조회
    Optional<NotificationVO> findById(@Param("notificationId") Long notificationId);

    // 사용자의 알림 목록을 페이징하여 조회
    List<NotificationVO> findNotificationsByUserId(
            @Param("userId") Long userId,
            @Param("readStatus") String readStatus,
            @Param("offset") int offset,
            @Param("size") int size
    );

    //특정 알림 읽음 처리
    void markAsRead(@Param("notificationId") Long notificationId);

    // 사용자의 모든 알림을 읽음 처리
    void markAllAsReadByUserId(@Param("userId") Long userId);
}
