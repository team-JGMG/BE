package org.bobj.common.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.bobj.common.constants.ErrorCode;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {

    @ApiModelProperty(value = "에러 발생 시각", example = "2025-07-06T13:00:11.744")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @ApiModelProperty(value = "HTTP 상태 코드", example = "400")
    private int status;

    @ApiModelProperty(value = "HTTP 상태 설명", example = "BAD_REQUEST")
    private String error;

    @ApiModelProperty(value = "서비스 에러 코드", example = "C001")
    private String code;

    @ApiModelProperty(value = "에러 메시지", example = "요약할 텍스트가 너무 깁니다.")
    private String message;

    @ApiModelProperty(value = "요청 경로", example = "/api/summaries")
    private String path;

    public static ErrorResponse of(ErrorCode errorCode, String customMessage, String path) {
        return ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(errorCode.getStatus().value())
            .error(errorCode.getStatus().name())
            .code(errorCode.getCode())
            .message(customMessage)
            .path(path)
            .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(errorCode.getStatus().value())
            .error(errorCode.getStatus().name())
            .code(errorCode.getCode())
            .message(errorCode.getMsg())
            .path(path)
            .build();
    }

    public static ErrorResponse from(HttpStatus status, String message, String path) {
        return ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error(status.name())
            .message(message)
            .path(path)
            .build();
    }
}
