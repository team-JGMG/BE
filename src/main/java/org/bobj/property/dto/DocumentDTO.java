package org.bobj.property.dto;

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
    private PropertyDocumentType documentType;
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
