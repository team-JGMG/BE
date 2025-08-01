package org.bobj.property.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 위도, 경도 좌표 정보를 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoordinateDTO {
    
    /**
     * 위도
     */
    private Double latitude;
    
    /**
     * 경도
     */
    private Double longitude;
}