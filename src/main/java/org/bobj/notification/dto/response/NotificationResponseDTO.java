package org.bobj.notification.dto.response;


import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.notification.domain.NotificationVO;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "알림 응답 DTO")
public class NotificationResponseDTO {

    @ApiModelProperty(value = "알림 ID", example = "1")
    private Long notificationId;

    @ApiModelProperty(value = "알림 제목", example = "거래가 체결되었어요!")
    private String title;

    @ApiModelProperty(value = "알림 내용", example = "5주가 10,000원에 체결되었습니다.")
    private String body;

//    @ApiModelProperty(value = "알림 유형", example = "TRADE_MATCHED")
//    private String type;

    @ApiModelProperty(value = "읽음 상태", example = "false")
    private boolean isRead;

    @ApiModelProperty(value = "생성 시간", example = "2025-08-05T10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public static NotificationResponseDTO of(NotificationVO vo) {
        return vo == null ? null : NotificationResponseDTO.builder()
                .notificationId(vo.getNotificationId())
                .title(vo.getTitle())
                .body(vo.getBody())
                .isRead(vo.getIsRead())
                .createdAt(vo.getCreatedAt())
                .build();
    }
}
