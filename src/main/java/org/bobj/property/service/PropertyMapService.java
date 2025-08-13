package org.bobj.property.service;

import org.bobj.property.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

//부동산 지도 관련 비즈니스 로직을 처리하는 서비스
@Slf4j
@Service
public class PropertyMapService {

    private final RealEstateApiService realEstateApiService;
    private final VWorldLocalApiService vworldLocalApiService;
    private final RedisTemplate<String, Object> redisTemplate;

    // 주소별 좌표 캐시 (메모리 캐시는 보조용으로만 사용)
    private final Map<String, CoordinateDTO> memoryCache = new HashMap<>();
    
    // Redis 캐시 키 설정
    private static final String COORDINATE_CACHE_PREFIX = "coordinate:";
    private static final long CACHE_EXPIRATION_HOURS = 24; // 24시간 캐시

    @Autowired
    public PropertyMapService(RealEstateApiService realEstateApiService,
                              VWorldLocalApiService vworldLocalApiService,
                              @Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.realEstateApiService = realEstateApiService;
        this.vworldLocalApiService = vworldLocalApiService;
        this.redisTemplate = redisTemplate;
        
        // Redis 사용 가능 여부 로그
        if (redisTemplate != null) {
            log.info("Redis 캐시 사용 가능 - 성능 최적화 모드");
        } else {
            log.warn("Redis 캐시 사용 불가 - 메모리 캐시만 사용");
        }
    }

    //주소를 받아서 좌표를 반환 (캐시 우선 조회)
    public CoordinateDTO getCoordinateFromAddress(String address) {
        try {
            // 메모리 캐시 확인
            if (memoryCache.containsKey(address)) {
                return memoryCache.get(address);
            }

            // Redis 캐시 확인
            CoordinateDTO cachedCoordinate = getCachedCoordinate(address);
            if (cachedCoordinate != null) {
                memoryCache.put(address, cachedCoordinate);
                return cachedCoordinate;
            }

            // API 호출
            CoordinateDTO coordinate = vworldLocalApiService.getCoordinateFromAddress(address);

            // 캐시에 저장
            setCachedCoordinate(address, coordinate);
            memoryCache.put(address, coordinate);

            return coordinate;

        } catch (Exception e) {
            log.error("주소 좌표 변환 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Redis에서 좌표 조회
     */
    private CoordinateDTO getCachedCoordinate(String address) {
        if (redisTemplate == null) {
            return null;
        }
        
        try {
            String key = COORDINATE_CACHE_PREFIX + address;
            Object cached = redisTemplate.opsForValue().get(key);
            return cached != null ? (CoordinateDTO) cached : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Redis에 좌표 저장
     */
    private void setCachedCoordinate(String address, CoordinateDTO coordinate) {
        if (redisTemplate == null) {
            return;
        }
        
        try {
            String key = COORDINATE_CACHE_PREFIX + address;
            redisTemplate.opsForValue().set(key, coordinate, CACHE_EXPIRATION_HOURS, java.util.concurrent.TimeUnit.HOURS);
        } catch (Exception e) {
            // Redis 실패해도 메모리 캐시는 동작하므로 계속 진행
        }
    }

    public List<RealEstateLocationDTO> getRealEstateLocations(String rawdCd) {
        try {
            log.info("실거래가 위치 정보 조회 시작 - 법정동코드: {}", rawdCd);
            
            List<RealEstateLocationDTO> locations = new ArrayList<>();
            Set<String> uniqueCoordinates = new HashSet<>();
            Set<String> processedAddresses = new HashSet<>();
            final int TARGET_LOCATIONS = 10;
            
            List<String> recentMonths = getRecentMonths();
            
            for (int monthIndex = 0; monthIndex < recentMonths.size(); monthIndex++) {
                String yearMonth = recentMonths.get(monthIndex);
                
                // 해당 월 실거래가 데이터 수집
                List<RealEstateTransactionDTO> monthlyTransactions = 
                    realEstateApiService.getRealEstateTransactions(rawdCd, yearMonth);
                
                if (monthlyTransactions.isEmpty()) {
                    continue;
                }
                
                // 주소별 그룹핑
                Map<String, List<RealEstateTransactionDTO>> groupedByAddress = 
                    monthlyTransactions.stream()
                        .collect(Collectors.groupingBy(
                            this::generateFullAddress,
                            LinkedHashMap::new,
                            Collectors.toList()
                        ));
                
                // 이미 처리한 주소 제외
                Map<String, List<RealEstateTransactionDTO>> newAddresses = groupedByAddress.entrySet()
                    .stream()
                    .filter(entry -> !processedAddresses.contains(entry.getKey()))
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                    ));
                
                // 효율성 체크 - 신규 주소가 부족하면 다음 달로 진행
                int remainingNeeded = TARGET_LOCATIONS - locations.size();
                if (newAddresses.size() < Math.max(2, remainingNeeded / 2) && monthIndex < recentMonths.size() - 1) {
                    processedAddresses.addAll(groupedByAddress.keySet());
                    continue;
                }
                
                // 좌표 변환 (병렬 처리)
                List<String> addressesToProcess = new ArrayList<>(newAddresses.keySet());
                ExecutorService executor = Executors.newFixedThreadPool(
                    Math.min(5, addressesToProcess.size())
                );
                
                try {
                    List<CompletableFuture<RealEstateLocationDTO>> futures = addressesToProcess.stream()
                        .map(address -> {
                            RealEstateTransactionDTO representative = newAddresses.get(address).get(0);
                            return CompletableFuture.supplyAsync(() -> {
                                try {
                                    return createLocationFromTransaction(
                                        representative, address, yearMonth);
                                } catch (Exception e) {
                                    log.error("좌표 변환 실패 - 주소: {}", address);
                                    return null;
                                }
                            }, executor);
                        })
                        .toList();
                    
                    // 결과 수집
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(30, java.util.concurrent.TimeUnit.SECONDS);
                    
                    // 좌표 중복 검사 및 수집
                    for (CompletableFuture<RealEstateLocationDTO> future : futures) {
                        if (locations.size() >= TARGET_LOCATIONS) {
                            break;
                        }
                        
                        try {
                            RealEstateLocationDTO location = future.get();
                            
                            if (location != null && location.getLatitude() != null && location.getLongitude() != null) {
                                String coordinateKey = String.format("%.4f,%.4f", 
                                    location.getLatitude(), location.getLongitude());
                                
                                if (!uniqueCoordinates.contains(coordinateKey)) {
                                    uniqueCoordinates.add(coordinateKey);
                                    locations.add(location);
                                }
                            }
                        } catch (Exception e) {
                            // 개별 결과 처리 실패는 무시
                        }
                    }
                    
                } catch (TimeoutException e) {
                    log.warn("좌표 변환 타임아웃 (30초), 부분 결과 사용");
                } finally {
                    executor.shutdown();
                    try {
                        if (!executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                            executor.shutdownNow();
                        }
                    } catch (InterruptedException e) {
                        executor.shutdownNow();
                        Thread.currentThread().interrupt();
                    }
                }
                
                processedAddresses.addAll(newAddresses.keySet());
                
                // 조기 종료 체크
                if (locations.size() >= TARGET_LOCATIONS) {
                    break;
                }
            }
            
            log.info("실거래가 위치 정보 수집 완료 - {}개 좌표 수집", locations.size());
            return locations;
            
        } catch (Exception e) {
            log.error("실거래가 위치 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    //최근 6개월 년월 목록 생성 (YYYYMM 형식)
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

    //실거래가 정보로부터 위치 정보 생성
    private RealEstateLocationDTO createLocationFromTransaction(
            RealEstateTransactionDTO transaction, String address, String yearMonth) {
        try {
            if (address == null || address.trim().isEmpty()) {
                return null;
            }

            // 캐시된 좌표 조회
            CoordinateDTO coordinate = getCoordinateFromAddress(address);

            if (coordinate == null || coordinate.getLatitude() == null || coordinate.getLongitude() == null) {
                return null;
            }

            return buildLocationDTO(transaction, coordinate, yearMonth);

        } catch (Exception e) {
            log.error("위치 정보 생성 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 좌표 정보로부터 응답 DTO 생성
     */
    private RealEstateLocationDTO buildLocationDTO(RealEstateTransactionDTO transaction, 
                                                 CoordinateDTO coordinate, String yearMonth) {
        if (coordinate == null || coordinate.getLatitude() == null || coordinate.getLongitude() == null) {
            return null;
        }

        String formattedYearMonth = yearMonth.substring(0, 4) + "-" + yearMonth.substring(4, 6);

        return RealEstateLocationDTO.builder()
                .latitude(coordinate.getLatitude())
                .longitude(coordinate.getLongitude())
                .dealAmount(transaction.getDealAmount())
                .dealYearMonth(formattedYearMonth)
                .build();
    }

    //전체 주소 생성 (실제 행정구역 사용, 아파트명 포함)
    public String generateFullAddress(RealEstateTransactionDTO transaction) {
        StringBuilder addressBuilder = new StringBuilder();

        // 실제 행정구역 사용
        if (transaction.getEstateAgentSggNm() != null && !transaction.getEstateAgentSggNm().trim().isEmpty()) {
            String realDistrict = transaction.getEstateAgentSggNm().trim();
            addressBuilder.append(realDistrict);

            // "서울 종로구" 형태를 "서울특별시 종로구"로 정규화
            if (realDistrict.startsWith("서울 ") && !realDistrict.contains("특별시")) {
                addressBuilder = new StringBuilder();
                addressBuilder.append(realDistrict.replace("서울 ", "서울특별시 "));
            }
        } else {
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

        // 아파트명 추가
        if (transaction.getAptNm() != null && !transaction.getAptNm().trim().isEmpty()) {
            String aptName = transaction.getAptNm().trim();
            if (!aptName.isEmpty()) {
                addressBuilder.append(" ").append(aptName);
            }
        }

        String fullAddress = addressBuilder.toString();

        // 빈 주소 방지
        if (fullAddress.trim().isEmpty()) {
            fullAddress = "서울특별시 중구";
        }

        return fullAddress;
    }
}
