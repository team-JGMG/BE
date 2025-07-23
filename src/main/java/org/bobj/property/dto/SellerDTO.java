package org.bobj.property.dto;

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
    private Long userId;
    private String name;
    private String phone;
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
