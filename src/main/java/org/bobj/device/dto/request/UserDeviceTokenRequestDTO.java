package org.bobj.device.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "디바이스 토큰 등록 요청 DTO")
public class UserDeviceTokenRequestDTO {
    @ApiModelProperty(value = "FCM 디바이스 토큰", required = true, example = "dbHbnjjAgOAxaUSNlnnfmD:APA91bG9CpdoIBr...")
    private String deviceToken;
}
