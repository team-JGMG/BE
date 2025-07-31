package org.bobj.property.service;

import org.bobj.property.dto.ApiErrorResponseDTO;
import org.bobj.property.dto.RealEstateApiResponseDTO;
import org.bobj.property.dto.RealEstateTransactionDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Collections;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 공공데이터포털 실거래가 API를 호출하는 서비스
 */
@Slf4j
@Service
public class RealEstateApiService {
    
    // 공공데이터포털 API 키 (application.properties에서 설정)
    @Value("${public.data.api.key:}")
    private String publicDataApiKey;
    
    // 공식 공공데이터포털 실거래가 API URL (XML 응답)
    private static final String REAL_ESTATE_API_URL = "https://apis.data.go.kr/1613000/RTMSDataSvcAptTrade/getRTMSDataSvcAptTrade";
    
    private final RestTemplate restTemplate;
    private final XmlMapper xmlMapper;
    
    public RealEstateApiService() {
        this.restTemplate = new RestTemplate();
        this.xmlMapper = new XmlMapper();
    }
    
    /**
     * 법정동코드를 이용하여 실거래가 정보를 조회
     * @param lawd_cd 법정동코드 (5자리)
     * @param dealYm 거래년월 (YYYYMM)
     * @return 실거래가 목록
     */
    public List<RealEstateTransactionDTO> getRealEstateTransactions(String lawd_cd, String dealYm) {
        try {
            log.info("실거래가 API 호출 - 법정동코드: {}, 거래년월: {}", lawd_cd, dealYm);
            
            // 🔧 완전한 이중인코딩 해결: 수동 인코딩으로 확실한 처리
            String encodedServiceKey = URLEncoder.encode(publicDataApiKey, StandardCharsets.UTF_8);
            
            // String.format을 사용해서 완전히 제어된 URL 생성
            String url = String.format("%s?serviceKey=%s&LAWD_CD=%s&DEAL_YMD=%s&numOfRows=%d&pageNo=%d",
                    REAL_ESTATE_API_URL,
                    encodedServiceKey,  // 수동으로 한번만 인코딩된 키
                    lawd_cd,
                    dealYm,
                    1000,
                    1
            );
            
            // URI 객체로 변환하여 RestTemplate이 재인코딩하지 않도록 처리
            URI uri = URI.create(url);
            log.debug("실거래가 API 요청 URI: {}", uri);
            
            // XML 응답을 String으로 받기 (URI 객체 사용)
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            String xmlResponse = response.getBody();
            
            if (xmlResponse == null || xmlResponse.trim().isEmpty()) {
                log.warn("빈 XML 응답 - 법정동코드: {}, 거래년월: {}", lawd_cd, dealYm);
                return Collections.emptyList();
            }
            
            log.debug("XML 응답 (처음 500자): {}", xmlResponse.substring(0, Math.min(500, xmlResponse.length())));
            
            // 에러 응답인지 먼저 확인
            if (xmlResponse.contains("OpenAPI_ServiceResponse")) {
                ApiErrorResponseDTO errorResponse = xmlMapper.readValue(xmlResponse, ApiErrorResponseDTO.class);
                
                String errorCode = errorResponse.getCmmMsgHeader().getReturnReasonCode();
                String errorMsg = errorResponse.getCmmMsgHeader().getReturnAuthMsg();
                String errMsg = errorResponse.getCmmMsgHeader().getErrMsg();
                
                log.error("공공데이터포털 API 오류 발생!");
                log.error("에러 코드: {} ({})", errorCode, getErrorCodeDescription(errorCode));
                log.error("에러 메시지: {}", errorMsg);
                log.error("상세 메시지: {}", errMsg);
                
                // 에러 코드별 해결 방법 안내
                logErrorSolution(errorCode);
                
                return Collections.emptyList();
            }
            
            // 정상 응답 파싱
            RealEstateApiResponseDTO responseDto = xmlMapper.readValue(xmlResponse, RealEstateApiResponseDTO.class);
            
            // 응답 검증
            if (responseDto.getHeader() != null) {
                String resultCode = responseDto.getHeader().getResultCode();
                String resultMsg = responseDto.getHeader().getResultMsg();
                
                log.info("API 응답 - 결과코드: {}, 메시지: {}", resultCode, resultMsg);
                
                if (!"00".equals(resultCode) && !"000".equals(resultCode)) {
                    log.warn("API 오류 응답 - 코드: {}, 메시지: {}", resultCode, resultMsg);
                    return Collections.emptyList();
                }
            }
            
            // 데이터 추출
            if (responseDto.getBody() != null && 
                responseDto.getBody().getItems() != null && 
                responseDto.getBody().getItems().getItem() != null) {
                
                List<RealEstateTransactionDTO> transactions = responseDto.getBody().getItems().getItem();
                
                log.info("실거래가 조회 성공 - 건수: {}", transactions.size());
                
                return transactions;
            } else {
                log.warn("실거래가 데이터가 없음 - 법정동코드: {}, 거래년월: {}", lawd_cd, dealYm);
                return Collections.emptyList();
            }
            
        } catch (Exception e) {
            log.error("실거래가 API 호출 중 오류 발생: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 에러 코드 설명 반환
     */
    private String getErrorCodeDescription(String errorCode) {
        switch (errorCode) {
            case "30": return "서비스키 미등록";
            case "31": return "서비스키 사용정지";
            case "32": return "서비스키 만료";
            case "33": return "등록되지 않은 IP";
            case "99": return "기타 오류";
            default: return "알 수 없는 오류";
        }
    }
    
    /**
     * 에러 코드별 해결 방법 안내
     */
    private void logErrorSolution(String errorCode) {
        switch (errorCode) {
            case "30":
                log.error("🔧 해결방법:");
                log.error("   1. 공공데이터포털(data.go.kr) 승인");
                break;
            case "31":
                log.error("🔧 해결방법: 공공데이터포털에서 서비스키 상태 확인");
                break;
            case "32":
                log.error("🔧 해결방법: 공공데이터포털에서 서비스키 갱신");
                break;
            case "33":
                log.error("🔧 해결방법: 공공데이터포털에서 IP 등록");
                break;
        }
    }
    
    /**
     * 최근 3개월간의 실거래가 정보를 조회
     * @param lawd_cd 법정동코드 (5자리)
     * @return 실거래가 목록
     */
    public List<RealEstateTransactionDTO> getRecentRealEstateTransactions(String lawd_cd) {
        List<RealEstateTransactionDTO> allTransactions = new java.util.ArrayList<>();
        
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
        
        // 최근 3개월 데이터 조회
        for (int i = 0; i < 3; i++) {
            LocalDate targetMonth = now.minusMonths(i);
            String dealYm = targetMonth.format(formatter);
            
            List<RealEstateTransactionDTO> monthlyTransactions = getRealEstateTransactions(lawd_cd, dealYm);
            allTransactions.addAll(monthlyTransactions);
        }
        
        return allTransactions;
    }
    
    /**
     * 특정 년월의 실거래가 정보를 조회
     * @param lawd_cd 법정동코드 (5자리)
     * @param year 년도
     * @param month 월
     * @return 실거래가 목록
     */
    public List<RealEstateTransactionDTO> getRealEstateTransactionsByYearMonth(String lawd_cd, int year, int month) {
        String dealYm = String.format("%04d%02d", year, month);
        return getRealEstateTransactions(lawd_cd, dealYm);
    }
}
