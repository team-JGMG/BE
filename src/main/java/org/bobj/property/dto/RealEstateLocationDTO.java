package org.bobj.property.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 실거래가 위치 정보 DTO (클라이언트 응답용 - 최소 정보만)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealEstateLocationDTO {

    /**
     * 위도
     */
    private Double latitude;

    /**
     * 경도
     */
    private Double longitude;

    /**
     * 거래금액 (만원)
     */
    private String dealAmount;
    
    /**
     * 거래년월 (YYYY-MM)
     */
    private String dealYearMonth;
}
