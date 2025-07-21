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
public class PropertyPhotoVO {
    private Long photoId;
    private Long propertyId;
    private String photoUrl;
    private Boolean isThumbnail;
    private LocalDateTime createdAt;
}
