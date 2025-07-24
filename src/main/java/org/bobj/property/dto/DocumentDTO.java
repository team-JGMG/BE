package org.bobj.property.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    public static DocumentDTO of(PropertyDocumentVO vo){
        return DocumentDTO.builder()
                .documentType(vo.getDocumentType())
                .fileUrl(vo.getFileUrl())
                .build();
    }

    public PropertyDocumentVO toVO(){
        return PropertyDocumentVO.builder()
                .documentType(documentType)
                .fileUrl(fileUrl)
                .build();
    }
}
