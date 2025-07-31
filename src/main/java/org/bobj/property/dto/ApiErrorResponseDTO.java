package org.bobj.property.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * 공공데이터포털 API 에러 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "OpenAPI_ServiceResponse")
public class ApiErrorResponseDTO {
    
    @JacksonXmlProperty(localName = "cmmMsgHeader")
    private ErrorHeader cmmMsgHeader;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorHeader {
        @JacksonXmlProperty(localName = "errMsg")
        private String errMsg;
        
        @JacksonXmlProperty(localName = "returnAuthMsg")
        private String returnAuthMsg;
        
        @JacksonXmlProperty(localName = "returnReasonCode")
        private String returnReasonCode;
    }
}
