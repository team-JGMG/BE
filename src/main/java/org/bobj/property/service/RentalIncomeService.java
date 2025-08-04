package org.bobj.property.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.bobj.property.dto.ApiErrorResponseDTO;
import org.bobj.property.dto.RentalResponseDTO;
import org.bobj.property.dto.RentalTransactionDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * 공공데이터포털 아파트 전월세 실거래가 API를 호출하는 서비스
 * API URL: https://apis.data.go.kr/1613000/RTMSDataSvcAptRent/getRTMSDataSvcAptRent
 */
@Slf4j
@Service
public class RentalIncomeService {
    
    // 주소 매칭 점수 상수
    private static final int SCORE_UMD_JIBUN_MATCH = 50;  // 법정동 + 지번 매칭
    private static final int SCORE_APT_NAME_MATCH = 20;   // 아파트명 매칭
    private static final int SCORE_UMD_ONLY_MATCH = 5;    // 읍면동명만 매칭
    private static final int SCORE_OTHER_MATCH = 1;       // 기타 키워드 매칭
    
    // 기본 월세 금액 (원 단위)
    private static final BigDecimal DEFAULT_MONTHLY_RENT = new BigDecimal("100");
    
    // API 호출 관련 상수
    private static final int API_CALL_DELAY_MS = 100;     // API 호출 간격 (0.1초)
    private static final int MAX_ROWS_PER_REQUEST = 1000; // 한 번에 조회할 최대 건수
    private static final int YEARLY_MONTHS = 12;          // 1년치 데이터 조회 개월 수
    
    // 공공데이터포털 API 키 (application.properties에서 설정)
    @Value("${public.data.api.key:}")
    private String publicDataApiKey;

    // 공식 공공데이터포털 아파트 전월세 실거래가 API URL (XML 응답)
    private static final String RENTAL_API_URL = "https://apis.data.go.kr/1613000/RTMSDataSvcAptRent/getRTMSDataSvcAptRent";

    private final RestTemplate restTemplate;
    private final XmlMapper xmlMapper;

    public RentalIncomeService() {
        this.restTemplate = new RestTemplate();
        this.xmlMapper = new XmlMapper();
    }

    /**
     * 법정동코드를 이용하여 아파트 전월세 실거래가 정보를 조회
     * @param lawd_cd 법정동코드 (5자리)
     * @param dealYm 거래년월 (YYYYMM)
     * @return 전월세 실거래가 목록
     */
    public List<RentalTransactionDTO> getRentalTransactions(String lawd_cd, String dealYm) {
        try {
            log.debug("아파트 전월세 실거래가 API 호출 - 법정동코드: {}, 거래년월: {}", lawd_cd, dealYm);

            // 완전한 이중인코딩 해결: 수동 인코딩으로 확실한 처리
            String encodedServiceKey = URLEncoder.encode(publicDataApiKey, StandardCharsets.UTF_8);

            // String.format을 사용해서 완전히 제어된 URL 생성
            String url = String.format("%s?serviceKey=%s&LAWD_CD=%s&DEAL_YMD=%s&numOfRows=%d&pageNo=%d",
                    RENTAL_API_URL,
                    encodedServiceKey,  // 수동으로 한번만 인코딩된 키
                    lawd_cd,
                    dealYm,
                    MAX_ROWS_PER_REQUEST,
                    1
            );

            // URI 객체로 변환하여 RestTemplate이 재인코딩하지 않도록 처리
            URI uri = URI.create(url);
            log.debug("전월세 실거래가 API 요청 URI: {}", uri);

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
                
                log.error("공공데이터포털 전월세 API 오류 발생!");
                log.error("에러 코드: {} ({})", errorCode, getErrorCodeDescription(errorCode));
                log.error("에러 메시지: {}", errorMsg);
                log.error("상세 메시지: {}", errMsg);
                
                // 에러 코드별 해결 방법 안내
                logErrorSolution(errorCode);
                
                return Collections.emptyList();
            }

            // 정상 응답 파싱 - 전월세 전용 DTO 사용
            RentalResponseDTO responseDto = xmlMapper.readValue(xmlResponse, RentalResponseDTO.class);

            // 응답 검증
            if (responseDto.getHeader() != null) {
                String resultCode = responseDto.getHeader().getResultCode();
                String resultMsg = responseDto.getHeader().getResultMsg();
                
                log.debug("전월세 API 응답 - 결과코드: {}, 메시지: {}", resultCode, resultMsg);

                if (!"00".equals(resultCode) && !"000".equals(resultCode)) {
                    log.warn("전월세 API 오류 응답 - 코드: {}, 메시지: {}", resultCode, resultMsg);
                    return Collections.emptyList();
                }
            }

            // 데이터 추출
            if (responseDto.getBody() != null &&
                    responseDto.getBody().getItems() != null &&
                    responseDto.getBody().getItems().getItem() != null) {

                List<RentalTransactionDTO> transactions = responseDto.getBody().getItems().getItem();
                
                log.info("전월세 실거래가 조회 성공 - 건수: {}", transactions.size());
                
                // 로그로 데이터 샘플 출력 (첫 번째 데이터만)
                if (!transactions.isEmpty()) {
                    RentalTransactionDTO sample = transactions.get(0);
                    log.debug("샘플 데이터 - 아파트: {}, 보증금: {}만원, 월세: {}만원, 전용면적: {}㎡", 
                             sample.getAptNm(), sample.getDeposit(), sample.getMonthlyRent(), sample.getExcluUseAr());
                }

                return transactions;
            } else {
                log.warn("전월세 실거래가 데이터가 없음 - 법정동코드: {}, 거래년월: {}", lawd_cd, dealYm);
                return Collections.emptyList();
            }

        } catch (Exception e) {
            log.error("전월세 실거래가 API 호출 중 오류 발생: {}", e.getMessage(), e);
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
     * 매물 등록 시 자동으로 rental_amount 계산을 위한 메서드
     * 6개월치 전월세 데이터를 조회하여 주소가 일치하는 가장 최근 월세 데이터의 월세금액을 반환
     * @param lawd_cd 법정동코드 (5자리)
     * @param propertyAddress 매물 주소
     * @return 가장 최근 월세 금액 (원 단위, 매칭 실패 시 기본값 100원)
     */
    public BigDecimal getLatestMonthlyRentForProperty(String lawd_cd, String propertyAddress) {
        try {
            log.info("매물 자동 월세 계산 시작 - 법정동코드: {}, 매물주소: {}", lawd_cd, propertyAddress);

            // 입력값 검증
            if (lawd_cd == null || lawd_cd.trim().isEmpty()) {
                log.error("법정동코드가 비어있음 - 기본값 {}원 반환", DEFAULT_MONTHLY_RENT);
                return DEFAULT_MONTHLY_RENT;
            }

            if (propertyAddress == null || propertyAddress.trim().isEmpty()) {
                log.error("매물 주소가 비어있음 - 기본값 {}원 반환", DEFAULT_MONTHLY_RENT);
                return DEFAULT_MONTHLY_RENT;
            }

            List<RentalTransactionDTO> yearlyTransactions = getYearlyRentalTransactions(lawd_cd);
            
            if (yearlyTransactions.isEmpty()) {
                log.warn("6개월치 전월세 데이터가 없음 - 법정동코드: {}, 기본값 {}원 반환", lawd_cd, DEFAULT_MONTHLY_RENT);
                return DEFAULT_MONTHLY_RENT;
            }

            // 월세 거래만 필터링 (월세금액이 0이 아닌 것들)
            List<RentalTransactionDTO> monthlyRentTransactions = yearlyTransactions.stream()
                    .filter(tx -> tx.getMonthlyRentAsLong() > 0)
                    .toList();

            if (monthlyRentTransactions.isEmpty()) {
                log.warn("월세 거래 데이터가 없음 - 법정동코드: {}, 기본값 {}원 반환", lawd_cd, DEFAULT_MONTHLY_RENT);
                return DEFAULT_MONTHLY_RENT;
            }

            log.info("전체 전월세 거래: {}건, 월세 거래: {}건", yearlyTransactions.size(), monthlyRentTransactions.size());

            // 주소 매칭 및 가장 최근 데이터 찾기
            RentalTransactionDTO bestMatch = findBestMatchingRentalData(monthlyRentTransactions, propertyAddress);

            if (bestMatch != null) {
                // 만원 단위를 원 단위로 변환 (x10000)
                BigDecimal monthlyRentInWon = new BigDecimal(bestMatch.getMonthlyRent()).multiply(new BigDecimal("10000"));
                log.info("매칭된 월세 데이터 - 아파트: {}, 주소: {}, 월세: {}만원 → {}원, 거래일: {}", 
                         bestMatch.getAptNm(), bestMatch.getFullAddress(), 
                         bestMatch.getMonthlyRent(), monthlyRentInWon, bestMatch.getDealDay());
                return monthlyRentInWon;
            } else {
                log.warn("주소가 매칭되는 월세 데이터를 찾을 수 없음 - 매물주소: {}, 기본값 {}원 적용", propertyAddress, DEFAULT_MONTHLY_RENT);
                return DEFAULT_MONTHLY_RENT;
            }

        } catch (IllegalArgumentException e) {
            log.error("잘못된 입력값으로 인한 월세 계산 실패: {}, 기본값 {}원 반환", e.getMessage(), DEFAULT_MONTHLY_RENT);
            return DEFAULT_MONTHLY_RENT;
        } catch (Exception e) {
            log.error("매물 자동 월세 계산 중 예상치 못한 오류 발생: {}, 기본값 {}원 반환", e.getMessage(), DEFAULT_MONTHLY_RENT, e);
            return DEFAULT_MONTHLY_RENT;
        }
    }

    /**
     * 6개월치 전월세 실거래가 데이터를 조회 (성능 최적화)
     * @param lawd_cd 법정동코드 (5자리)
     * @return 6개월치 전월세 실거래가 목록
     */
    private List<RentalTransactionDTO> getYearlyRentalTransactions(String lawd_cd) {
        List<RentalTransactionDTO> allTransactions = new java.util.ArrayList<>();
        
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
        
        log.info("6개월치 전월세 실거래가 조회 시작 - 법정동코드: {}", lawd_cd);
        
        // 최근 6개월 데이터 조회 (성능 최적화)
        for (int i = 0; i < 6; i++) {
            LocalDate targetMonth = now.minusMonths(i);
            String dealYm = targetMonth.format(formatter);
            
            List<RentalTransactionDTO> monthlyTransactions = getRentalTransactions(lawd_cd, dealYm);
            allTransactions.addAll(monthlyTransactions);
            
            // API 호출 간격 (과도한 요청 방지)
            if (i < 5) {
                try {
                    Thread.sleep(API_CALL_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("API 호출 대기 중 인터럽트 발생");
                    break;
                }
            }
        }
        
        log.info("6개월치 전월세 실거래가 조회 완료 - 총 건수: {}", allTransactions.size());
        
        return allTransactions;
    }

    /**
     * 매물 주소와 가장 일치하는 최신 월세 데이터를 찾는 메서드
     * @param monthlyRentTransactions 월세 거래 목록
     * @param propertyAddress 매물 주소
     * @return 가장 적합한 월세 거래 데이터
     */
    private RentalTransactionDTO findBestMatchingRentalData(List<RentalTransactionDTO> monthlyRentTransactions, String propertyAddress) {
        RentalTransactionDTO bestMatch = null;
        int bestScore = 0;
        String latestDealDate = "";

        // 매물 주소에서 키워드 추출 (아파트명, 동명 등)
        String[] propertyKeywords = extractAddressKeywords(propertyAddress);
        
        log.debug("매물 주소 키워드: {}", String.join(", ", propertyKeywords));

        for (RentalTransactionDTO transaction : monthlyRentTransactions) {
            int matchScore = calculateAddressMatchScore(transaction, propertyKeywords);
            String dealDate = transaction.getDealDay();

            // 점수가 더 높거나, 점수가 같으면서 더 최근 데이터인 경우
            if (matchScore > bestScore || 
                (matchScore == bestScore && matchScore > 0 && dealDate.compareTo(latestDealDate) > 0)) {
                bestMatch = transaction;
                bestScore = matchScore;
                latestDealDate = dealDate;
            }
        }

        if (bestMatch != null) {
            log.debug("최종 선택된 데이터 - 매칭점수: {}, 아파트: {}, 거래일: {}", 
                     bestScore, bestMatch.getAptNm(), bestMatch.getDealDay());
        }

        return bestMatch;
    }

    /**
     * 주소에서 매칭에 사용할 키워드들을 추출
     * @param address 주소
     * @return 키워드 배열
     */
    private String[] extractAddressKeywords(String address) {
        if (address == null || address.trim().isEmpty()) {
            return new String[0];
        }

        // 주소를 공백과 특수문자로 분리하여 키워드 추출
        String cleanAddress = address.replaceAll("[^가-힣a-zA-Z0-9\\s]", " ");
        String[] keywords = cleanAddress.trim().split("\\s+");
        
        // 2글자 이상인 키워드만 사용 (의미있는 키워드 추출)
        return java.util.Arrays.stream(keywords)
                .filter(keyword -> keyword.length() >= 2)
                .toArray(String[]::new);
    }

    /**
     * 전월세 거래 데이터와 매물 주소의 매칭 점수 계산
     * 1순위: 법정동 + 지번 매칭 (50점)
     * 2순위: 아파트명 매칭 (20점)
     * 기타: 읍면동명 매칭 (5점), 기타 키워드 매칭 (1점)
     * @param transaction 전월세 거래 데이터
     * @param propertyKeywords 매물 주소 키워드들
     * @return 매칭 점수 (높을수록 일치도 높음)
     */
    private int calculateAddressMatchScore(RentalTransactionDTO transaction, String[] propertyKeywords) {
        int score = 0;
        String fullTransactionAddress = transaction.getFullAddress().toLowerCase();
        String aptName = transaction.getAptNm() != null ? transaction.getAptNm().toLowerCase() : "";
        String umdName = transaction.getUmdNm() != null ? transaction.getUmdNm().toLowerCase() : "";
        String jibun = transaction.getJibun() != null ? transaction.getJibun().toLowerCase() : "";

        // 법정동 + 지번 매칭 체크 (1순위 - 50점)
        boolean umdMatched = false;
        boolean jibunMatched = false;
        
        for (String keyword : propertyKeywords) {
            String lowerKeyword = keyword.toLowerCase();
            
            // 읍면동명 매칭 체크
            if (umdName.contains(lowerKeyword) && lowerKeyword.length() >= 2) {
                umdMatched = true;
            }
            
            // 지번 매칭 체크 (숫자 포함 키워드)
            if (jibun.contains(lowerKeyword) && lowerKeyword.matches(".*\\d.*")) {
                jibunMatched = true;
            }
        }
        
        // 법정동 + 지번 둘 다 매칭되면 최고 점수
        if (umdMatched && jibunMatched) {
            score += SCORE_UMD_JIBUN_MATCH;
            log.debug("법정동+지번 매칭 (+{}점) - 읍면동: {}, 지번: {}", SCORE_UMD_JIBUN_MATCH, umdName, jibun);
        }

        // 개별 키워드별 점수 계산
        for (String keyword : propertyKeywords) {
            String lowerKeyword = keyword.toLowerCase();
            
            // 아파트명에 키워드가 포함되어 있으면 높은 점수 (2순위)
            if (aptName.contains(lowerKeyword) && lowerKeyword.length() >= 2) {
                score += SCORE_APT_NAME_MATCH;
                log.debug("아파트명 매칭 (+{}점) - 키워드: {}, 아파트: {}", SCORE_APT_NAME_MATCH, lowerKeyword, aptName);
            }
            // 읍면동명에 키워드가 포함되어 있으면 중간 점수 (이미 법정동+지번으로 점수를 받지 않은 경우)
            else if (umdName.contains(lowerKeyword) && !umdMatched) {
                score += SCORE_UMD_ONLY_MATCH;
                log.debug("읍면동명 매칭 (+{}점) - 키워드: {}, 읍면동: {}", SCORE_UMD_ONLY_MATCH, lowerKeyword, umdName);
            }
            // 전체 주소에 키워드가 포함되어 있으면 낮은 점수
            else if (fullTransactionAddress.contains(lowerKeyword)) {
                score += SCORE_OTHER_MATCH;
                log.debug("기타 주소 매칭 (+{}점) - 키워드: {}", SCORE_OTHER_MATCH, lowerKeyword);
            }
        }

        return score;
    }

    /**
     * 에러 코드별 해결 방법 안내
     */
    private void logErrorSolution(String errorCode) {
        switch (errorCode) {
            case "30":
                log.error("🔧 해결방법: 공공데이터포털(data.go.kr)에서 아파트 전월세 실거래가 API 승인 필요");
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
}
