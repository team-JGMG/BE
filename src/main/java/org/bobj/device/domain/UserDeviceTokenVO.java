package org.bobj.device.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDeviceTokenVO {

    private Long userDeviceTokenId;
    private Long userId;
    private String deviceToken;
    private LocalDateTime createdAt;
}
