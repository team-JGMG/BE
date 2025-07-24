package org.bobj.property.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerVO {
    private Long userId;          // user_id BIGINT
    private String name;          // name VARCHAR(100)
    private String phone;         // phone VARCHAR(20)
    private String email;         // email VARCHAR(255)
}
