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

    public static PhotoDTO of(PropertyPhotoVO vo, S3Service s3Service) {
        return PhotoDTO.builder()
                .photoUrl(s3Service.generatePresignedUrl(getS3KeyFromUrl(vo.getPhotoUrl())))
                .build();
    }

    // S3 키 추출 메서드 (내부에서만 사용되므로 private static)
    // 예: https://s3.amazonaws.com/your-bucket/uploads/abc.pdf
    // → uploads/abc.pdf 추출
    private static String getS3KeyFromUrl(String url) {
        return url.substring(url.indexOf(".com/") + 5); // uploads/abc.pdf
    }

    public PropertyPhotoVO toVO(){
        return PropertyPhotoVO.builder()
                .photoUrl(photoUrl)
                .build();
    }
}
