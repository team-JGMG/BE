package org.bobj.property.service;

import org.bobj.property.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 부동산 지도 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
public class PropertyMapService {

    private final RealEstateApiService realEstateApiService;
    private final VWorldLocalApiService vworldLocalApiService;

    // 주소별 좌표 캐시 (같은 주소 중복 호출 방지)
    private final Map<String, CoordinateDTO> addressCache = new HashMap<>();

    @Autowired
    public PropertyMapService(RealEstateApiService realEstateApiService,
                              VWorldLocalApiService vworldLocalApiService) {
        this.realEstateApiService = realEstateApiService;
        this.vworldLocalApiService = vworldLocalApiService;
    }

    /**
     * 주소를 받아서 좌표를 반환
     *
     * @param address 주소
     * @return 좌표 정보
     */
    public CoordinateDTO getCoordinateFromAddress(String address) {
        try {
            log.info("주소 좌표 변환 요청: {}", address);

            CoordinateDTO coordinate = vworldLocalApiService.getCoordinateFromAddress(address);

            if (coordinate != null) {
                log.info("좌표 변환 성공 - 주소: {}, 위도: {}, 경도: {}", address, coordinate.getLatitude(), coordinate.getLongitude());
            } else {
                log.warn("좌표 변환 실패 - 주소: {}", address);
            }

            return coordinate;

        } catch (Exception e) {
            log.error("주소 좌표 변환 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    public List<RealEstateLocationDTO> getRealEstateLocations(String rawdCd) {
        try {
            log.info("실거래가 위치 정보 조회 시작 - 법정동코드: {} (최대 50개 수집)", rawdCd);

            // 캐시 초기화 (정확한 주소 형식 적용을 위해)
            addressCache.clear();
            log.info("주소 캐시 초기화 완료 - 새로운 정확한 주소 형식 적용");

            List<RealEstateLocationDTO> locations = new ArrayList<>();
            List<String> recentMonths = getRecentMonths();
            final int MAX_LOCATIONS = 50; // 최대 수집 개수 제한

            // 최신 월부터 순차적으로 처리
            for (String yearMonth : recentMonths) {
                // 이미 50개 수집했으면 중단
                if (locations.size() >= MAX_LOCATIONS) {
                    log.info("최대 수집 개수({})에 도달하여 데이터 수집 중단", MAX_LOCATIONS);
                    break;
                }

                List<RealEstateTransactionDTO> monthlyTransactions = realEstateApiService.getRealEstateTransactions(rawdCd, yearMonth);
                log.info("{} 조회된 실거래가 건수: {} (현재 수집: {}/{})",
                        yearMonth, monthlyTransactions.size(), locations.size(), MAX_LOCATIONS);

                for (RealEstateTransactionDTO transaction : monthlyTransactions) {
                    // 50개 수집 완료시 즉시 중단
                    if (locations.size() >= MAX_LOCATIONS) {
                        log.info("최대 수집 개수({})에 도달 - {}월 처리 중단", MAX_LOCATIONS, yearMonth);
                        break;
                    }

                    try {
                        RealEstateLocationDTO location = createLocationFromTransaction(transaction, yearMonth);
                        if (location != null) {
                            locations.add(location);
                            log.debug("실거래가 위치 정보 추가 - 현재 수집: {}/{}", locations.size(), MAX_LOCATIONS);
                        }

                    } catch (Exception e) {
                        log.error("좌표 변환 중 오류: {}", e.getMessage());
                        // 개별 오류는 무시하고 계속 진행
                    }
                }

                // 해당 월 처리 완료 로그
                log.info("{} 처리 완료 - 현재 총 수집: {}/{}", yearMonth, locations.size(), MAX_LOCATIONS);
            }

            log.info("실거래가 위치 정보 수집 완료 - 총 {}건 (최대 {}건)", locations.size(), MAX_LOCATIONS);
            return locations;

        } catch (Exception e) {
            log.error("실거래가 위치 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 최근 6개월 년월 목록 생성 (YYYYMM 형식)
     */
    private List<String> getRecentMonths() {
        List<String> months = new ArrayList<>();
        LocalDate now = LocalDate.now();
        
        for (int i = 0; i < 3; i++) {
            LocalDate targetDate = now.minusMonths(i);
            String yearMonth = targetDate.format(DateTimeFormatter.ofPattern("yyyyMM"));
            months.add(yearMonth);
        }

        return months;
    }

    /**
     * 실거래가 정보로부터 위치 정보 생성 (최소 정보만)
     * @param transaction 실거래가 정보
     * @param yearMonth 거래년월 (YYYYMM)  
     * @return 위치 정보
     */
    private RealEstateLocationDTO createLocationFromTransaction(RealEstateTransactionDTO transaction, String yearMonth) {
        try {
            // 주소 생성 (좌표 변환용)
            String address = generateFullAddress(transaction);
            
            // 빈 주소 검증
            if (address == null || address.trim().isEmpty()) {
                log.warn("빈 주소로 인해 좌표 조회 생략");
                return null;
            }
            
            CoordinateDTO coordinate = null;
            
            // 캐시에서 확인 (중복 주소 방지)
            if (addressCache.containsKey(address)) {
                coordinate = addressCache.get(address);
                log.info("캐시에서 좌표 조회: {} -> {}", address, coordinate);
            } else {
                log.info("브이월드 API 호출 시작: {}", address);
                coordinate = vworldLocalApiService.getCoordinateFromAddress(address);
                
                // 캐시에 저장 (null도 캐시해서 중복 호출 방지)
                addressCache.put(address, coordinate);
                log.info("좌표 캐시 저장: {} -> {}", address, coordinate);
            }
            
            // 좌표를 찾지 못한 경우 폴백 처리
            if (coordinate == null) {
                log.warn("좌표를 찾을 수 없음 - 주소: {}", address);

                // 법정동코드별 대표 좌표 사용
                coordinate = getFallbackCoordinate(address);
                if (coordinate != null) {
                    log.info("폴백 좌표 사용: {} -> 위도: {}, 경도: {}", address, coordinate.getLatitude(), coordinate.getLongitude());
                } else {
                    return null; // 폴백도 없으면 null 반환
                }
            }

            // 년월 포맷 변환 (202507 -> 2025-07)
            String formattedYearMonth = yearMonth.substring(0, 4) + "-" + yearMonth.substring(4, 6);

            // 클라이언트에게 보낼 최소 정보만 설정
            return RealEstateLocationDTO.builder()
                    .latitude(coordinate.getLatitude())
                    .longitude(coordinate.getLongitude())
                    .dealAmount(transaction.getDealAmount())
                    .dealYearMonth(formattedYearMonth)
                    .build();
                    
        } catch (Exception e) {
            log.error("위치 정보 생성 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 주소별 폴백 좌표 제공
     */
    private CoordinateDTO getFallbackCoordinate(String address) {
        // 서울 중구 지역별 대표 좌표
        Map<String, CoordinateDTO> fallbackCoordinates = Map.of(
                "창신동", CoordinateDTO.builder().latitude(37.5751).longitude(127.0106).build(), // 종로구 창신동
                "사직동", CoordinateDTO.builder().latitude(37.5759).longitude(126.9730).build(), // 종로구 사직동
                "내수동", CoordinateDTO.builder().latitude(37.5658).longitude(126.9784).build(), // 중구 내수동
                "홍파동", CoordinateDTO.builder().latitude(37.5650).longitude(126.9850).build(), // 중구 추정
                "명동", CoordinateDTO.builder().latitude(37.5636).longitude(126.9856).build(),   // 중구 명동
                "중구", CoordinateDTO.builder().latitude(37.5636).longitude(126.9756).build()     // 서울 중구청
        );

        // 주소에서 동명 찾기
        for (String dong : fallbackCoordinates.keySet()) {
            if (address.contains(dong)) {
                log.info("폴백 좌표 매핑: {} -> {}", dong, fallbackCoordinates.get(dong));
                return fallbackCoordinates.get(dong);
            }
        }

        // 기본 폴백: 서울 중구청
        log.info("기본 폴백 좌표 사용: 서울 중구청");
        return fallbackCoordinates.get("중구");
    }

    /**
     * 전체 주소 생성 (실제 행정구역 사용)
     *
     * @param transaction 거래 정보
     * @return 전체 주소
     */
    private String generateFullAddress(RealEstateTransactionDTO transaction) {
        StringBuilder addressBuilder = new StringBuilder();

        log.debug("주소 생성 시작 - estateAgentSggNm: {}, umdNm: {}, jibun: {}, aptNm: {}",
                transaction.getEstateAgentSggNm(), transaction.getUmdNm(), transaction.getJibun(), transaction.getAptNm());

        //실제 행정구역 사용 (estateAgentSggNm 우선)
        if (transaction.getEstateAgentSggNm() != null && !transaction.getEstateAgentSggNm().trim().isEmpty()) {
            String realDistrict = transaction.getEstateAgentSggNm().trim();
            addressBuilder.append(realDistrict);

            // "서울 종로구" 형태를 "서울특별시 종로구"로 정규화
            if (realDistrict.startsWith("서울 ") && !realDistrict.contains("특별시")) {
                addressBuilder = new StringBuilder();
                addressBuilder.append(realDistrict.replace("서울 ", "서울특별시 "));
            }
        } else {
            // 폴백: 기본 중구 사용
            addressBuilder.append("서울특별시 중구");
        }

        // 읍면동명 추가
        if (transaction.getUmdNm() != null && !transaction.getUmdNm().trim().isEmpty()) {
            addressBuilder.append(" ").append(transaction.getUmdNm().trim());
        }

        // 지번 추가
        if (transaction.getJibun() != null && !transaction.getJibun().trim().isEmpty()) {
            addressBuilder.append(" ").append(transaction.getJibun().trim());
        }

        String fullAddress = addressBuilder.toString();
        log.info("생성된 정확한 주소: {}", fullAddress);

        // 빈 주소 방지
        if (fullAddress.trim().isEmpty()) {
            fullAddress = "서울특별시 중구";
            log.warn("빈 주소 감지, 기본 주소 사용: {}", fullAddress);
        }

        return fullAddress;
    }
}
