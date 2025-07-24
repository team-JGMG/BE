package org.bobj.property.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.property.domain.SellerVO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerDTO {
    @ApiModelProperty(value = "판매자 ID", example = "1")
    private Long userId;
    @ApiModelProperty(value = "판매자 이름", example = "홍길동")
    private String name;
    @ApiModelProperty(value = "전화번호", example = "010-1234-5678")
    private String phone;
    @ApiModelProperty(value = "이메일", example = "hong@example.com")
    private String email;

    public static SellerDTO of(SellerVO vo){
        return SellerDTO.builder()
                .userId(vo.getUserId())
                .name(vo.getName())
                .phone(vo.getPhone())
                .email(vo.getEmail())
                .build();
    }

    public SellerVO toVO(){
        return SellerVO.builder()
                .userId(userId)
                .name(name)
                .phone(phone)
                .email(email)
                .build();
    }
}
