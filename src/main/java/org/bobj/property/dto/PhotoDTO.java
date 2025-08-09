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
        String url = vo.getPhotoUrl();

        // 1. 테스트 URL이나 잘못된 값이면 기본 이미지 반환
        if (url == null || url.contains("example.com")) {
            return PhotoDTO.builder()
                    .photoUrl("https://s3.example.com/photo1.jpg") // 로컬 기본 이미지 경로 또는 CDN 경로
                    .build();
        }

        String key = getS3KeyFromUrl(url);

        try {
            String originalFilename = s3Service.getOriginalFilenameFromS3(key);
            return PhotoDTO.builder()
                    .photoUrl(s3Service.generatePresignedUrl(key, originalFilename))
                    .build();
        } catch (Exception e) {
            // 2. S3에서 못 찾으면 기본 이미지 반환
            return PhotoDTO.builder()
                    .photoUrl("https://s3.example.com/photo1.jpg")
                    .build();
        }
    }

    // S3 키 추출 메서드 (내부에서만 사용되므로 private static)
    // 예: https://s3.amazonaws.com/your-bucket/uploads/abc.pdf
    // → uploads/abc.pdf 추출
    private static String getS3KeyFromUrl(String url) {
        return url.substring(url.indexOf(".com/") + 5);
    }
}
