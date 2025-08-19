package org.bobj.property.service;

import org.bobj.property.dto.*;
import org.bobj.property.util.GeoUtils;
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

//부동산 지도 관련 비즈니스 로직을 처리하는 서비스 (좌표 변환 + 실거래가 위치 조회)
@Slf4j
@Service
public class PropertyMapService {

    private final RealEstateApiService realEstateApiService;
    private final VWorldLocalApiService vworldLocalApiService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PropertyService propertyService;

    // 주소별 좌표 캐시 (메모리 캐시는 보조용으로만 사용)
    private final Map<String, CoordinateDTO> memoryCache = new HashMap<>();
    private final Map<String, CoordinateDTO> negativeCache = new HashMap<>(); // 실패 결과 캐싱
    
    // Redis 캐시 키 설정
    private static final String COORDINATE_CACHE_PREFIX = "coordinate:";
    private static final String NEGATIVE_CACHE_PREFIX = "coordinate:failed:"; // 실패 캐시
    private static final String REALESTATE_CACHE_PREFIX = "realestate:"; // 실거래가 데이터 캐시
    private static final long CACHE_EXPIRATION_HOURS = 24; // 24시간 캐시
    private static final long NEGATIVE_CACHE_EXPIRATION_HOURS = 1; // 실패 캐시 1시간
    private static final long REALESTATE_CACHE_EXPIRATION_DAYS = 7; // 실거래가 캐시 7일
    
    // 반경 기반 필터링 설정
    private static final double RADIUS_KM = 1.0;           // 고정 1km 반경
    private static final int MIN_TRANSACTION_COUNT = 10;    // 최소 필요 개수
    private static final int TARGET_RETURN_COUNT = 20;     // 목표 반환 개수

    @Autowired
    public PropertyMapService(RealEstateApiService realEstateApiService,
                              VWorldLocalApiService vworldLocalApiService,
                              RedisTemplate<String, Object> redisTemplate,
                              PropertyService propertyService) {
        this.realEstateApiService = realEstateApiService;
        this.vworldLocalApiService = vworldLocalApiService;
        this.redisTemplate = redisTemplate;
        this.propertyService = propertyService;
        
        // Redis 연결 테스트 (Order 패턴과 동일하게)
        try {
            // Redis 쓰기/읽기 테스트로 실제 연결 확인
            redisTemplate.opsForValue().set("test:propertymap:init", "connected", 10, java.util.concurrent.TimeUnit.SECONDS);
            String testResult = (String) redisTemplate.opsForValue().get("test:propertymap:init");
            if ("connected".equals(testResult)) {
                log.info("순수 수동 Redis 캐시 설정 초기화 시작");
                log.info("Redis 캐시 연결 성공 - 성능 최적화 모드 활성화");
                redisTemplate.delete("test:propertymap:init"); // 테스트 키 정리
            } else {
                log.warn("Redis 읽기 실패 - 메모리 캐시만 사용");
            }
        } catch (Exception e) {
            log.error("Redis 캐시 연결 실패 - 메모리 캐시만 사용: {}", e.getMessage());
            // Redis 실패해도 애플리케이션은 계속 동작 (메모리 캐시 사용)
        }
    }

    //주소를 받아서 좌표를 반환 (캐시 우선 조회)
    public CoordinateDTO getCoordinateFromAddress(String address) {
        try {
            // 주소 정규화 (캐시 키 통일)
            String normalizedAddress = normalizeAddress(address);
            
            // 실패 캐시 확인 (불필요한 API 호출 방지)
            if (isInNegativeCache(normalizedAddress)) {
                log.debug("실패 캐시에서 제외 - 주소: {}", normalizedAddress);
                return null;
            }
            
            // 메모리 캐시 확인
            if (memoryCache.containsKey(normalizedAddress)) {
                log.debug("메모리 캐시 히트 - 주소: {}", normalizedAddress);
                return memoryCache.get(normalizedAddress);
            }

            // Redis 캐시 확인
            CoordinateDTO cachedCoordinate = getCachedCoordinate(normalizedAddress);
            if (cachedCoordinate != null) {
                log.debug("Redis 캐시 히트 - 주소: {}", normalizedAddress);
                memoryCache.put(normalizedAddress, cachedCoordinate);
                return cachedCoordinate;
            }

            // API 호출 (최적화된 방식)
            CoordinateDTO coordinate = getCoordinateFromApiWithOptimization(normalizedAddress);

            if (coordinate != null && coordinate.getLatitude() != null && coordinate.getLongitude() != null) {
                // 성공 캐시에 저장
                setCachedCoordinate(normalizedAddress, coordinate);
                memoryCache.put(normalizedAddress, coordinate);
                log.debug("API 호출 성공 및 캐시 저장 - 주소: {}", normalizedAddress);
            } else {
                // 실패 캐시에 저장 (1시간)
                setNegativeCache(normalizedAddress);
                log.debug("API 호출 실패, 실패 캐시 저장 - 주소: {}", normalizedAddress);
            }

            return coordinate;

        } catch (Exception e) {
            log.error("주소 좌표 변환 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    //주소 정규화 (단순화)
    private String normalizeAddress(String address) {
        if (address == null) {
            return "";
        }
        
        return address.trim()
                .replaceAll("\\s+", " ")  // 연속 공백을 하나로
                .toLowerCase(); // 대소문자 통일
    }

    /**
     * 단순화된 API 호출 (모든 주소 동일 방식: parcel → road)
     */
    private CoordinateDTO getCoordinateFromApiWithOptimization(String address) {
        // 1차: parcel 타입으로 시도
        CoordinateDTO coordinate = vworldLocalApiService.getCoordinateFromAddressWithType(address, "parcel");
        if (coordinate != null && coordinate.getLatitude() != null) {
            return coordinate;
        }
        
        // 2차: road 타입으로 시도
        coordinate = vworldLocalApiService.getCoordinateFromAddressWithType(address, "road");
        return coordinate;
    }

    //실패 캐시 확인
    private boolean isInNegativeCache(String address) {
        // 메모리 실패 캐시 확인
        if (negativeCache.containsKey(address)) {
            return true;
        }
        
        // Redis 실패 캐시 확인
        if (redisTemplate != null) {
            try {
                String key = NEGATIVE_CACHE_PREFIX + address;
                return redisTemplate.hasKey(key);
            } catch (Exception e) {
                return false;
            }
        }
        
        return false;
    }

    //실패 캐시 저장
    private void setNegativeCache(String address) {
        // 메모리 실패 캐시
        negativeCache.put(address, null);
        
        // Redis 실패 캐시
        if (redisTemplate != null) {
            try {
                String key = NEGATIVE_CACHE_PREFIX + address;
                redisTemplate.opsForValue().set(key, "FAILED", NEGATIVE_CACHE_EXPIRATION_HOURS, java.util.concurrent.TimeUnit.HOURS);
            } catch (Exception e) {
                // Redis 실패해도 메모리 캐시는 동작하므로 계속 진행
            }
        }
    }

    //Redis에서 좌표 조회 (정규화된 주소 사용)
    private CoordinateDTO getCachedCoordinate(String normalizedAddress) {
        if (redisTemplate == null) {
            return null;
        }
        
        try {
            String key = COORDINATE_CACHE_PREFIX + normalizedAddress;
            Object cached = redisTemplate.opsForValue().get(key);
            return cached != null ? (CoordinateDTO) cached : null;
        } catch (Exception e) {
            return null;
        }
    }

    //Redis에 좌표 저장 (정규화된 주소 사용)
    private void setCachedCoordinate(String normalizedAddress, CoordinateDTO coordinate) {
        if (redisTemplate == null) {
            return;
        }
        
        try {
            String key = COORDINATE_CACHE_PREFIX + normalizedAddress;
            redisTemplate.opsForValue().set(key, coordinate, CACHE_EXPIRATION_HOURS, java.util.concurrent.TimeUnit.HOURS);
        } catch (Exception e) {
            // Redis 실패해도 메모리 캐시는 동작하므로 계속 진행
        }
    }

    /**
     * 캐시된 실거래가 데이터 조회 (Redis 우선)
     */
    private List<RealEstateTransactionDTO> getCachedRealEstateTransactions(String rawdCd, String yearMonth) {
        if (redisTemplate == null) {
            return null; // Redis 없으면 직접 API 호출
        }
        
        try {
            String key = REALESTATE_CACHE_PREFIX + rawdCd + ":" + yearMonth;
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("실거래가 Redis 캐시 히트 - {}:{}", rawdCd, yearMonth);
                return (List<RealEstateTransactionDTO>) cached;
            }
            return null;
        } catch (Exception e) {
            log.warn("실거래가 캐시 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 실거래가 데이터 캐시 저장 (Redis)
     */
    private void setCachedRealEstateTransactions(String rawdCd, String yearMonth, List<RealEstateTransactionDTO> transactions) {
        if (redisTemplate == null || transactions == null) {
            return;
        }
        
        try {
            String key = REALESTATE_CACHE_PREFIX + rawdCd + ":" + yearMonth;
            redisTemplate.opsForValue().set(key, transactions, REALESTATE_CACHE_EXPIRATION_DAYS, java.util.concurrent.TimeUnit.DAYS);
            log.debug("실거래가 데이터 캐시 저장 - {}:{}, {}건", rawdCd, yearMonth, transactions.size());
        } catch (Exception e) {
            log.warn("실거래가 캐시 저장 실패: {}", e.getMessage());
        }
    }

    /**
     * 캐시 우선 실거래가 데이터 조회
     */
    private List<RealEstateTransactionDTO> getRealEstateTransactionsWithCache(String rawdCd, String yearMonth) {
        // 1차: Redis 캐시 확인
        List<RealEstateTransactionDTO> cached = getCachedRealEstateTransactions(rawdCd, yearMonth);
        if (cached != null) {
            return cached;
        }
        
        // 2차: API 호출 및 캐시 저장
        List<RealEstateTransactionDTO> transactions = realEstateApiService.getRealEstateTransactions(rawdCd, yearMonth);
        
        // 성공적으로 데이터를 받았으면 캐시에 저장
        if (transactions != null && !transactions.isEmpty()) {
            setCachedRealEstateTransactions(rawdCd, yearMonth, transactions);
        }
        
        return transactions != null ? transactions : Collections.emptyList();
    }
    private List<String> getRecentMonths() {
        List<String> months = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = 0; i < 6; i++) {
            LocalDate targetDate = now.minusMonths(i);
            String yearMonth = targetDate.format(DateTimeFormatter.ofPattern("yyyyMM"));
            months.add(yearMonth);
        }

        return months;
    }


    //좌표 정보로부터 응답 DTO 생성
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

    //전체 주소 생성 (estateAgentSggNm + umdNm + jibun + aptNm)
    public String generateFullAddress(RealEstateTransactionDTO transaction) {
        StringBuilder addressBuilder = new StringBuilder();

        // 1. estateAgentSggNm (쉼표 있으면 첫 번째만)
        if (transaction.getEstateAgentSggNm() != null && !transaction.getEstateAgentSggNm().trim().isEmpty()) {
            String district = transaction.getEstateAgentSggNm().trim();
            
            // 쉼표 있으면 첫 번째만 사용
            if (district.contains(",")) {
                district = district.split(",")[0].trim();
            }
            
            addressBuilder.append(district);
        }

        // 2. umdNm
        if (transaction.getUmdNm() != null && !transaction.getUmdNm().trim().isEmpty()) {
            addressBuilder.append(" ").append(transaction.getUmdNm().trim());
        }

        // 3. jibun
        if (transaction.getJibun() != null && !transaction.getJibun().trim().isEmpty()) {
            addressBuilder.append(" ").append(transaction.getJibun().trim());
        }

        // 4. aptNm
        if (transaction.getAptNm() != null && !transaction.getAptNm().trim().isEmpty()) {
            addressBuilder.append(" ").append(transaction.getAptNm().trim());
        }

        String fullAddress = addressBuilder.toString().trim();
        
        // 빈 주소 방지
        if (fullAddress.isEmpty()) {
            fullAddress = null;
        }

        return fullAddress;
    }

     //펀딩 ID로 매물 좌표 조회 (기존 메소드들 조합)
    public CoordinateDTO getPropertyCoordinateByFundingId(Long fundingId) {
        try {
            log.info("펀딩 ID로 매물 좌표 조회 - 펀딩ID: {}", fundingId);

            // 1단계: 펀딩 ID → 매물 주소 (기존 로직)
            String propertyAddress = propertyService.getPropertyAddressByFundingId(fundingId);

            if (propertyAddress == null || propertyAddress.trim().isEmpty()) {
                throw new RuntimeException("매물 주소를 찾을 수 없습니다. fundingId: " + fundingId);
            }

            // 2단계: 주소 → 좌표 변환 (기존 로직)
            CoordinateDTO coordinate = getCoordinateFromAddress(propertyAddress);

            if (!GeoUtils.isValidCoordinate(coordinate.getLatitude(), coordinate.getLongitude())) {
                throw new RuntimeException("유효하지 않은 좌표입니다. address: " + propertyAddress);
            }

            log.info("매물 좌표 조회 성공 - 펀딩ID: {}, 좌표: ({}, {})",
                    fundingId, coordinate.getLatitude(), coordinate.getLongitude());
            return coordinate;

        } catch (Exception e) {
            log.error("매물 좌표 조회 중 오류 발생 - 펀딩ID: {}, 오류: {}", fundingId, e.getMessage(), e);
            throw new RuntimeException("매물 좌표 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    //안전한 매물 좌표 조회
    private CoordinateDTO getPropertyCoordinateSafely(Long fundingId) {
        try {
            CoordinateDTO coordinate = getPropertyCoordinateByFundingId(fundingId);
            log.info("매물 중심 좌표: ({}, {})", coordinate.getLatitude(), coordinate.getLongitude());
            return coordinate;
        } catch (Exception e) {
            log.error("매물 좌표 조회 실패, 반경 필터링 없이 진행: {}", e.getMessage());
            return null;
        }
    }


    public List<RealEstateLocationDTO> getRealEstateLocationsWithAdaptiveRadius(String rawdCd, Long fundingId) {
        try {
            log.info("시간적 확장 기반 실거래가 위치 정보 조회 시작 - 법정동코드: {}, 펀딩ID: {}", rawdCd, fundingId);

            List<RealEstateLocationDTO> locations = new ArrayList<>();
            Set<String> uniqueCoordinates = new HashSet<>();
            Set<String> processedAddresses = new HashSet<>();
            final int TARGET_LOCATIONS = TARGET_RETURN_COUNT; // 월별 처리 제한

            // 1단계: 매물 좌표 조회 (반경 필터링용)
            final CoordinateDTO propertyCoordinate = getPropertyCoordinateSafely(fundingId);

            List<String> recentMonths = getRecentMonths();
            boolean shouldTerminateAfterThisMonth = false; // 현재 월 처리 후 종료 플래그

            for (int monthIndex = 0; monthIndex < recentMonths.size(); monthIndex++) {
                String yearMonth = recentMonths.get(monthIndex);

                log.info("{}개월차 데이터 수집 중 - {}, 현재 수집: {}개",
                        monthIndex + 1, yearMonth, locations.size());

                // 2단계: 해당 월 실거래가 데이터 수집 (캐시 우선)
                List<RealEstateTransactionDTO> monthlyTransactions =
                    getRealEstateTransactionsWithCache(rawdCd, yearMonth);

                if (monthlyTransactions.isEmpty()) {
                    log.info("{}월 데이터 없음 - 다음 월로 진행", yearMonth);
                    continue;
                }

                // 3단계: 주소별 그룹핑 (중복 제거)
                Map<String, List<RealEstateTransactionDTO>> groupedByAddress =
                    monthlyTransactions.stream()
                        .collect(Collectors.groupingBy(
                            this::generateFullAddress,
                            LinkedHashMap::new,
                            Collectors.toList()
                        ));

                // 4단계: 이미 처리한 주소 제외
                Map<String, List<RealEstateTransactionDTO>> newAddresses = groupedByAddress.entrySet()
                    .stream()
                    .filter(entry -> !processedAddresses.contains(entry.getKey()))
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                    ));

                if (newAddresses.isEmpty()) {
                    log.info("{}월 신규 주소 없음 - 다음 월로 진행", yearMonth);
                    continue;
                }

                log.info("{}월 신규 주소 {}개 발견", yearMonth, newAddresses.size());

                // 5단계: 좌표 변환 (병렬 처리) - 주소별로 1번씩만
                List<String> addressesToProcess = new ArrayList<>(newAddresses.keySet());
                ExecutorService executor = Executors.newFixedThreadPool(
                    Math.min(5, addressesToProcess.size())
                );

                try {
                    List<CompletableFuture<RealEstateLocationWithDistance>> futures = addressesToProcess.stream()
                        .map(address -> {
                            RealEstateTransactionDTO representative = newAddresses.get(address).get(0);
                            return CompletableFuture.supplyAsync(() -> {
                                try {
                                    return createLocationWithDistanceCheck(
                                        representative, address, yearMonth, propertyCoordinate);
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

                    // 6단계: 1km 반경 필터링 및 좌표 중복 검사
                    for (CompletableFuture<RealEstateLocationWithDistance> future : futures) {
                        // 20개 달성시에만 즉시 중단 (하드 리미트)
                        if (locations.size() >= TARGET_RETURN_COUNT) {
                            log.info("최대 개수 달성으로 즉시 종료 - {}개", locations.size());
                            return locations;
                        }

                        try {
                            RealEstateLocationWithDistance locationWithDistance = future.get();

                            if (locationWithDistance != null && locationWithDistance.location != null) {
                                RealEstateLocationDTO location = locationWithDistance.location;

                                // 1km 반경 필터링 (매물 좌표가 있는 경우에만)
                                if (propertyCoordinate != null && locationWithDistance.distance != null) {
                                    if (locationWithDistance.distance > RADIUS_KM) {
                                        continue; // 1km 밖이면 제외
                                    }
                                }

                                // 좌표 중복 체크 (정밀도 강화: 소수점 6자리)
                                if (location.getLatitude() != null && location.getLongitude() != null) {
                                    String coordinateKey = String.format("%.6f,%.6f",
                                        location.getLatitude(), location.getLongitude());

                                    if (!uniqueCoordinates.contains(coordinateKey)) {
                                        uniqueCoordinates.add(coordinateKey);
                                        locations.add(location);

                                        // 10개 달성 체크: 즉시 종료하지 않고 현재 월 완료 후 종료
                                        if (!shouldTerminateAfterThisMonth && locations.size() >= MIN_TRANSACTION_COUNT) {
                                            shouldTerminateAfterThisMonth = true;
                                            log.info("최소 개수 달성! {}개월차 데이터 모두 처리 후 종료 예정 - 현재: {}개",
                                                    monthIndex + 1, locations.size());
                                        }
                                    }
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
                
                log.info("{}월 처리 완료 - 현재 누적: {}개", yearMonth, locations.size());
                
                // 10개 달성 월 완료 체크
                if (shouldTerminateAfterThisMonth) {
                    log.info("목표 달성 월 처리 완료로 수집 종료 - 최종 {}개 ({}개월차 데이터 모두 처리 완료)", 
                            locations.size(), monthIndex + 1);
                    break;
                }
            }
            
            log.info("시간적 확장 기반 실거래가 위치 정보 수집 완료 - {}개 좌표 수집 (1km 반경 고정, 10개 달성 월까지 완전 처리)",
                    locations.size());
            return locations;
            
        } catch (Exception e) {
            log.error("시간적 확장 기반 실거래가 위치 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 좌표 변환과 거리 계산을 함께 수행하는 내부 클래스
     */
    private static class RealEstateLocationWithDistance {
        RealEstateLocationDTO location;
        Double distance; // 매물로부터의 거리 (km)
        
        RealEstateLocationWithDistance(RealEstateLocationDTO location, Double distance) {
            this.location = location;
            this.distance = distance;
        }
    }

    /**
     * 실거래가 정보로부터 위치 정보 생성 + 거리 계산
     */
    private RealEstateLocationWithDistance createLocationWithDistanceCheck(
            RealEstateTransactionDTO transaction, String address, String yearMonth, CoordinateDTO propertyCoordinate) {
        try {
            if (address == null || address.trim().isEmpty()) {
                return null;
            }

            // 좌표 변환 (캐시 활용)
            CoordinateDTO coordinate = getCoordinateFromAddress(address);

            if (coordinate == null || coordinate.getLatitude() == null || coordinate.getLongitude() == null) {
                return null;
            }

            // 위치 정보 생성
            RealEstateLocationDTO location = buildLocationDTO(transaction, coordinate, yearMonth);
            
            // 거리 계산 (매물 좌표가 있는 경우에만)
            Double distance = null;
            if (propertyCoordinate != null && 
                GeoUtils.isValidCoordinate(propertyCoordinate.getLatitude(), propertyCoordinate.getLongitude())) {
                distance = GeoUtils.calculateDistance(
                    propertyCoordinate.getLatitude(), propertyCoordinate.getLongitude(),
                    coordinate.getLatitude(), coordinate.getLongitude()
                );
            }

            return new RealEstateLocationWithDistance(location, distance);

        } catch (Exception e) {
            log.error("위치 정보 생성 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }


}
