package org.bobj.user.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description = "간단한 응답 DTO")
public class SimpleResponseDTO {

    @ApiModelProperty(value = "응답 메시지", example = "로그아웃 되었습니다.", required = true)
    private String message;

    @ApiModelProperty(value = "성공 여부", example = "true", required = true)
    private Boolean success;

    @ApiModelProperty(value = "추가 데이터", required = false)
    private Map<String, Object> data;

    @ApiModelProperty(value = "응답 시각", example = "2025-07-27T15:30:00", required = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    // 정적 팩토리 메소드들
    public static SimpleResponseDTO success(String message) {
        return SimpleResponseDTO.builder()
                .message(message)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static SimpleResponseDTO success(String message, Map<String, Object> data) {
        return SimpleResponseDTO.builder()
                .message(message)
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static SimpleResponseDTO error(String message) {
        return SimpleResponseDTO.builder()
                .message(message)
                .success(false)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static SimpleResponseDTO error(String message, Map<String, Object> data) {
        return SimpleResponseDTO.builder()
                .message(message)
                .success(false)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
