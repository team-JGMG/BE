package org.bobj.property.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;

/**
 * 공공데이터포털 아파트 전월세 실거래가 API XML 응답 DTO
 * API URL: https://apis.data.go.kr/1613000/RTMSDataSvcAptRent/getRTMSDataSvcAptRent
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "response")
public class RentalResponseDTO {

    @JacksonXmlProperty(localName = "header")
    private HeaderData header;

    @JacksonXmlProperty(localName = "body")
    private BodyData body;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeaderData {
        @JacksonXmlProperty(localName = "resultCode")
        private String resultCode;

        @JacksonXmlProperty(localName = "resultMsg")
        private String resultMsg;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BodyData {
        @JacksonXmlProperty(localName = "items")
        private ItemsData items;

        @JacksonXmlProperty(localName = "numOfRows")
        private Integer numOfRows;

        @JacksonXmlProperty(localName = "pageNo")
        private Integer pageNo;

        @JacksonXmlProperty(localName = "totalCount")
        private Integer totalCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemsData {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "item")
        private List<RentalTransactionDTO> item;  // 전월세 전용 DTO 사용
    }
}
