package org.bobj.property.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.property.domain.PropertyPhotoVO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoDTO {
    private String photoUrl;

    public static PhotoDTO of(PropertyPhotoVO vo){
        return PhotoDTO.builder()
                .photoUrl(vo.getPhotoUrl())
                .build();
    }

    public PropertyPhotoVO toVO(){
        return PropertyPhotoVO.builder()
                .photoUrl(photoUrl)
                .build();
    }
}
