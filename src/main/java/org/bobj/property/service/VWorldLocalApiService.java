package org.bobj.property.service;

import org.bobj.property.dto.CoordinateDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@Slf4j
@Service
public class VWorldLocalApiService {
    
    // 브이월드 API 키 (공공데이터포털에서 발급)
    @Value("${vworld.api.key:YOUR_VWORLD_API_KEY}")
    private String vworldApiKey;
    
    private static final String VWORLD_GEOCODER_URL = "https://api.vworld.kr/req/address";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public VWorldLocalApiService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    

    public CoordinateDTO getCoordinateFromAddress(String address) {
        try {
            // 빈 주소 검증
            if (address == null || address.trim().isEmpty()) {
                log.warn("빈 주소로 인해 브이월드 API 호출 생략");
                return null;
            }
            
            log.debug("브이월드 API 주소 검색 요청: {}", address);
            
            // API 키 존재 여부 확인
            if (vworldApiKey == null || vworldApiKey.trim().isEmpty() || "YOUR_VWORLD_API_KEY".equals(vworldApiKey)) {
                log.error("브이월드 API 키가 설정되지 않았습니다!");
                return null;
            }

            // 여러 주소 형태로 시도
            CoordinateDTO result = null;
            
            //원본 주소로 지번 검색
            result = tryGeocoding(address, "parcel");
            if (result != null) return result;
            
            //원본 주소로 도로명 검색
            result = tryGeocoding(address, "road");
            if (result != null) return result;
            
            //"번지" 추가해서 지번 검색
            String addressWithBeonji = address.replaceAll("(\\d+)$", "$1번지");
            if (!addressWithBeonji.equals(address)) {
                log.debug("번지 추가 시도: {}", addressWithBeonji);
                result = tryGeocoding(addressWithBeonji, "parcel");
                if (result != null) return result;
            }
            
            //동명만으로 검색 (예: "창신동")
            String dongOnly = extractDong(address);
            if (dongOnly != null) {
                log.debug("동명만으로 시도: {}", dongOnly);
                result = tryGeocoding(dongOnly, "parcel");
                if (result != null) return result;
            }
            
            log.warn("브이월드 API에서 주소를 찾을 수 없음: {}", address);
            return null;
            
        } catch (Exception e) {
            log.error("브이월드 API 호출 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }
    
    //주소를 좌표로 변환
    public CoordinateDTO getCoordinateFromAddressWithType(String address, String type) {
        try {
            // 빈 주소 검증
            if (address == null || address.trim().isEmpty()) {
                log.warn("빈 주소로 인해 브이월드 API 호출 생략");
                return null;
            }
            
            log.debug("브이월드 API 주소 검색 요청 ({}): {}", type, address);
            
            // API 키 존재 여부 확인
            if (vworldApiKey == null || vworldApiKey.trim().isEmpty() || "YOUR_VWORLD_API_KEY".equals(vworldApiKey)) {
                log.error("브이월드 API 키가 설정되지 않았습니다!");
                return null;
            }

            // 지정된 타입으로만 시도
            CoordinateDTO result = tryGeocoding(address, type);
            if (result != null) {
                return result;
            }
            
            log.debug("브이월드 API에서 주소를 찾을 수 없음 ({}): {}", type, address);
            return null;
            
        } catch (Exception e) {
            log.error("브이월드 API 호출 중 오류 발생 ({}): {}", type, e.getMessage(), e);
            return null;
        }
    }
    

    //실제 지오코딩 API 호출
    private CoordinateDTO tryGeocoding(String address, String type) {
        try {
            // URL 생성
            String url = UriComponentsBuilder.fromHttpUrl(VWORLD_GEOCODER_URL)
                    .queryParam("service", "address")
                    .queryParam("request", "getCoord")
                    .queryParam("version", "2.0")
                    .queryParam("crs", "epsg:4326")
                    .queryParam("address", address)
                    .queryParam("format", "json")
                    .queryParam("type", type)
                    .queryParam("refine", "true")  // 주소 정제 활성화
                    .queryParam("simple", "false")
                    .queryParam("key", vworldApiKey)
                    .build()
                    .toUriString();
            
            log.debug("브이월드 API 요청 ({}) URL: {}", type, url);
            
            // API 호출
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String responseBody = response.getBody();
            
            if (responseBody != null) {
                // JSON 파싱
                JsonNode rootNode = objectMapper.readTree(responseBody);
                JsonNode responseNode = rootNode.get("response");
                
                if (responseNode != null) {
                    String status = responseNode.get("status").asText();
                    
                    if ("OK".equals(status)) {
                        JsonNode resultNode = responseNode.get("result");
                        if (resultNode != null) {
                            JsonNode pointNode = resultNode.get("point");
                            if (pointNode != null) {
                                String x = pointNode.get("x").asText(); // 경도
                                String y = pointNode.get("y").asText(); // 위도
                                
                                Double longitude = Double.parseDouble(x);
                                Double latitude = Double.parseDouble(y);
                                
                                log.info("좌표 변환 성공 ({}) - 주소: {}, 위도: {}, 경도: {}", type, address, latitude, longitude);
                                
                                return CoordinateDTO.builder()
                                        .latitude(latitude)
                                        .longitude(longitude)
                                        .build();
                            }
                        }
                    } else if ("NOT_FOUND".equals(status)) {
                        log.debug("주소 찾을 수 없음 ({}): {}", type, address);
                    } else {
                        log.warn("브이월드 API 오류 ({}) - 상태: {}", type, status);
                    }
                }
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("브이월드 API 호출 중 오류 발생 ({}): {}", type, e.getMessage());
            return null;
        }
    }
    

    //주소에서 동명 추출
    private String extractDong(String address) {
        try {
            // "서울특별시 중구 창신동 703" -> "서울특별시 중구 창신동"
            if (address.contains("동 ")) {
                return address.substring(0, address.lastIndexOf("동 ") + 1);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    

    public java.util.List<CoordinateDTO> getCoordinatesFromAddresses(java.util.List<String> addresses) {
        return addresses.stream()
                .map(this::getCoordinateFromAddress)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
    }
}
