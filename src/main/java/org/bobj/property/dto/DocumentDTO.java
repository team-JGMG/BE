package org.bobj.property.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.common.s3.S3Service;
import org.bobj.property.domain.PropertyDocumentType;
import org.bobj.property.domain.PropertyDocumentVO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentDTO {
    @ApiModelProperty(value = "문서 유형", example = "LICENSE")
    private PropertyDocumentType documentType;

    @ApiModelProperty(value = "파일 URL", example = "https://example.com/document.pdf")
    private String fileUrl;

    public static DocumentDTO of(PropertyDocumentVO vo, S3Service s3Service) {
        String key = getS3KeyFromUrl(vo.getFileUrl());
        String originalFilename = s3Service.getOriginalFilenameFromS3(key);

        return DocumentDTO.builder()
                .documentType(vo.getDocumentType())
                .fileUrl(s3Service.generatePresignedUrl(key, originalFilename))
                .build();
    }

    // S3 키 추출 메서드 (내부에서만 사용되므로 private static)
    // 예: https://s3.amazonaws.com/your-bucket/uploads/abc.pdf
    // → uploads/abc.pdf 추출
    private static String getS3KeyFromUrl(String url) {
        return url.substring(url.indexOf(".com/") + 5); // uploads/abc.pdf
    }
}
