package org.bobj.property.controller;

import org.bobj.property.dto.CoordinateDTO;
import org.bobj.property.dto.RealEstateLocationDTO;
import org.bobj.property.service.PropertyMapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

/**
 * 부동산 지도 관련 API를 제공하는 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/property/map")
@CrossOrigin(origins = "*")
public class PropertyMapController {
    
    private final PropertyMapService propertyMapService;
    
    @Autowired
    public PropertyMapController(PropertyMapService propertyMapService) {
        this.propertyMapService = propertyMapService;
    }
    
    /**
     * 주소를 받아서 좌표를 반환하는 API
     * 
     * @param address 주소
     * @return 좌표 정보
     */
    @PostMapping("/coordinate")
    public ResponseEntity<CoordinateDTO> getCoordinateFromAddress(@RequestBody String address) {
        
        try {
            log.info("주소 좌표 변환 요청: {}", address);
            
            // 입력값 검증 및 전처리
            if (address == null || address.trim().isEmpty()) {
                log.warn("잘못된 주소: {}", address);
                return ResponseEntity.badRequest().build();
            }
            
            // 주소 문자열 정리 (앞뒤 따옴표 제거, 공백 정리)
            String cleanAddress = address.trim();
            if (cleanAddress.startsWith("\"") && cleanAddress.endsWith("\"")) {
                cleanAddress = cleanAddress.substring(1, cleanAddress.length() - 1);
            }
            cleanAddress = cleanAddress.trim();
            
            log.info("정리된 주소: {}", cleanAddress);
            
            // 좌표 조회
            CoordinateDTO coordinate = propertyMapService.getCoordinateFromAddress(cleanAddress);
            
            if (coordinate != null) {
                log.info("주소 좌표 변환 성공: {}", cleanAddress);
                return ResponseEntity.ok(coordinate);
            } else {
                log.warn("좌표를 찾을 수 없음: {}", cleanAddress);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("주소 좌표 변환 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 법정동코드를 이용하여 최근 3개월 실거래가와 좌표 정보를 조회
     * 
     * @param rawdCd 법정동코드 (5자리)
     * @return 실거래가와 좌표 정보 목록
     */
    @GetMapping("/real-estate-locations")
    public ResponseEntity<List<RealEstateLocationDTO>> getRealEstateLocations(
            @RequestParam("rawdCd") String rawdCd) {
        
        try {
            log.info("최근 3개월 실거래가 위치 정보 요청 - 법정동코드: {}", rawdCd);
            
            // 입력값 검증
            if (rawdCd == null || rawdCd.trim().isEmpty()) {
                log.warn("잘못된 법정동코드: {}", rawdCd);
                return ResponseEntity.badRequest().build();
            }
            
            // 법정동코드 형식 검증 (5자리 숫자)
            if (!rawdCd.matches("\\d{5}")) {
                log.warn("법정동코드 형식 오류: {}", rawdCd);
                return ResponseEntity.badRequest().build();
            }
            
            // 실거래가 위치 정보 조회
            List<RealEstateLocationDTO> locations = propertyMapService.getRealEstateLocations(rawdCd);
            
            log.info("최근 3개월 실거래가 위치 정보 응답 - 총 {}건", locations.size());
            
            return ResponseEntity.ok(locations);
            
        } catch (Exception e) {
            log.error("최근 3개월 실거래가 위치 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 헬스체크 엔드포인트
     * @return 상태 정보
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Property Map API is running");
    }
}
