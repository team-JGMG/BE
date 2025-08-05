package org.bobj.notification.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "FCM 알림 전송 요청 DTO")
public class FcmRequestDto {

    @ApiModelProperty(value = "알림을 받을 디바이스의 FCM 토큰", example = "dEviC3t0KeN1234567890", required = true)
    private String deviceToken;

    @ApiModelProperty(value = "알림 제목", example = "새로운 메시지가 도착했습니다", required = true)
    private String title;

    @ApiModelProperty(value = "알림 본문 내용", example = "채팅방에서 새로운 메시지가 도착했습니다.", required = true)
    private String body;

}

