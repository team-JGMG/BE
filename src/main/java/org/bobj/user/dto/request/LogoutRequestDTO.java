package org.bobj.user.dto.request;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "로그아웃 요청 DTO")
public class LogoutRequestDTO {

    @ApiModelProperty(value = "로그아웃할 기기의 디바이스 토큰 (선택 사항)", example = "f3vI0L2...fJc7V4F", required = false)
    private String deviceToken;
}