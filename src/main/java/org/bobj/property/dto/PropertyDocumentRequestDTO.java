package org.bobj.property.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bobj.property.domain.PropertyDocumentType;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyDocumentRequestDTO {
    private MultipartFile file; // 실제 업로드할 파일
    private PropertyDocumentType type; // 문서 타입 (enum으로 전달)
}
