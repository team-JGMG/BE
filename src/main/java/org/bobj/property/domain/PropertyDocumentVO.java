package org.bobj.property.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyDocumentVO {
    private Long documentId;
    private Long propertyId;
    private String documentType; // ENUM('OWNERSHIP_CERTIFICATE','SEAL_CERTIFICATE', ...)
    private String fileUrl;
    private LocalDateTime createdAt;
}
