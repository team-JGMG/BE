package org.bobj.property.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 부동산 실거래가 데이터를 담는 DTO (XML 파싱용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // 알 수 없는 필드 무시
public class RealEstateTransactionDTO {
    
    @JacksonXmlProperty(localName = "aptNm")
    private String aptNm;  // 아파트명 (주소 생성용)

    @JacksonXmlProperty(localName = "dealAmount")
    private String dealAmount;  // 거래금액

    @JacksonXmlProperty(localName = "jibun")
    private String jibun;  // 지번 (주소 생성용)

    @JacksonXmlProperty(localName = "umdNm")
    private String umdNm;  // 읍면동명 (주소 생성용)
    
    @JacksonXmlProperty(localName = "estateAgentSggNm")
    private String estateAgentSggNm;  // 실제 행정구역 (예: "서울 종로구")

}
