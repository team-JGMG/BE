package org.bobj.property.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.common.s3.S3Service;
import org.bobj.property.domain.PropertyPhotoVO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoDTO {
    @ApiModelProperty(value = "사진 URL", example = "https://example.com/image.jpg")
    private String photoUrl;

    public static PhotoDTO of(PropertyPhotoVO vo) {
        return PhotoDTO.builder()
                .photoUrl(vo.getPhotoUrl())
                .build();
    }
}
