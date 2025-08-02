package org.bobj.property.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 아파트 전월세 실거래가 API 응답 아이템 DTO
 * API URL: https://apis.data.go.kr/1613000/RTMSDataSvcAptRent/getRTMSDataSvcAptRent
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RentalTransactionDTO {

    @JacksonXmlProperty(localName = "aptNm")
    private String aptNm;  // 아파트명

    @JacksonXmlProperty(localName = "buildYear")
    private String buildYear;  // 건축년도

    @JacksonXmlProperty(localName = "contractTerm")
    private String contractTerm;  // 계약기간

    @JacksonXmlProperty(localName = "contractType")
    private String contractType;  // 계약구분 (신규, 갱신)

    @JacksonXmlProperty(localName = "dealDay")
    private String dealDay;  // 계약일

    @JacksonXmlProperty(localName = "deposit")
    private String deposit;  // 보증금 (만원)

    @JacksonXmlProperty(localName = "excluUseAr")
    private String excluUseAr;  // 전용면적

    @JacksonXmlProperty(localName = "floor")
    private String floor;  // 층

    @JacksonXmlProperty(localName = "jibun")
    private String jibun;  // 지번

    @JacksonXmlProperty(localName = "monthlyRent")
    private String monthlyRent;  // 월세금 (만원)

    @JacksonXmlProperty(localName = "preDeposit")
    private String preDeposit;  // 종전 보증금 (만원)

    @JacksonXmlProperty(localName = "preMonthlyRent")
    private String preMonthlyRent;  // 종전 월세금 (만원)

    @JacksonXmlProperty(localName = "sggCd")
    private String sggCd;  // 시군구코드

    @JacksonXmlProperty(localName = "umdNm")
    private String umdNm;  // 읍면동명

    @JacksonXmlProperty(localName = "useRRRight")
    private String useRRRight;  // 갱신요구권사용 (Y/N)
    
    // 편의 메서드들
    
    /**
     * 완전한 주소를 생성합니다
     */
    public String getFullAddress() {
        StringBuilder address = new StringBuilder();
        if (umdNm != null && !umdNm.trim().isEmpty()) {
            address.append(umdNm).append(" ");
        }
        if (jibun != null && !jibun.trim().isEmpty()) {
            address.append(jibun).append(" ");
        }
        if (aptNm != null && !aptNm.trim().isEmpty()) {
            address.append(aptNm);
        }
        return address.toString().trim();
    }
    
    /**
     * 보증금을 숫자로 반환 (만원 단위)
     * 안전한 파싱으로 NumberFormatException 방지
     */
    public Long getDepositAsLong() {
        if (deposit == null || deposit.trim().isEmpty()) {
            return 0L;
        }
        try {
            String cleaned = deposit.replace(",", "").trim();
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            log.warn("보증금 파싱 실패: '{}' - 기본값 0 반환", deposit);
            return 0L;
        }
    }
    
    /**
     * 월세를 숫자로 반환 (만원 단위)
     * 안전한 파싱으로 NumberFormatException 방지
     */
    public Long getMonthlyRentAsLong() {
        if (monthlyRent == null || monthlyRent.trim().isEmpty()) {
            return 0L;
        }
        try {
            String cleaned = monthlyRent.replace(",", "").trim();
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            log.warn("월세 파싱 실패: '{}' - 기본값 0 반환", monthlyRent);
            return 0L;
        }
    }
    
    /**
     * 전용면적을 숫자로 반환 (㎡)
     * 안전한 파싱으로 NumberFormatException 방지
     */
    public Double getExcluUseArAsDouble() {
        if (excluUseAr == null || excluUseAr.trim().isEmpty()) {
            return 0.0;
        }
        try {
            String cleaned = excluUseAr.replace(",", "").trim();
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            log.warn("전용면적 파싱 실패: '{}' - 기본값 0.0 반환", excluUseAr);
            return 0.0;
        }
    }
    
    /**
     * 평형으로 변환 (1평 = 3.3058㎡)
     */
    public Double getExcluUseArInPyeong() {
        return getExcluUseArAsDouble() / 3.3058;
    }
    
    /**
     * 전월세 구분 반환 (전세/월세)
     */
    public String getRentalType() {
        Long monthlyRentAmount = getMonthlyRentAsLong();
        if (monthlyRentAmount == 0) {
            return "전세";
        } else {
            return "월세";
        }
    }
}
