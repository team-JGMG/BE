package org.bobj.property.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.bobj.property.dto.CoordinateDTO;
import org.bobj.property.dto.RealEstateLocationDTO;
import org.bobj.property.service.PropertyMapService;
import org.bobj.property.service.PropertyService;
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
@Api(tags = "부동산 지도 API")
public class PropertyMapController {

    private final PropertyMapService propertyMapService;
    private final PropertyService propertyService;

    @Autowired
    public PropertyMapController(PropertyMapService propertyMapService, PropertyService propertyService) {
        this.propertyMapService = propertyMapService;
        this.propertyService = propertyService;
    }

    /**
     * 주소를 받아서 좌표를 반환하는 API
     *
     * @param address 주소
     * @return 좌표 정보
     */
//    @PostMapping("/coordinate")
//    @ApiOperation(value = "주소 좌표 변환",
//                  notes = "주소를 받아서 위도, 경도 좌표로 변환합니다.")
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "변환 성공", response = CoordinateDTO.class),
//            @ApiResponse(code = 400, message = "잘못된 주소 형식"),
//            @ApiResponse(code = 404, message = "주소에 해당하는 좌표를 찾을 수 없음"),
//            @ApiResponse(code = 500, message = "서버 내부 오류")
//    })
//    public ResponseEntity<CoordinateDTO> getCoordinateFromAddress(
//            @ApiParam(value = "변환할 주소", required = true, example = "서울특별시 강남구 테헤란로 427")
//            @RequestBody String address) {
//
//        try {
//            log.info("주소 좌표 변환 요청: {}", address);
//
//            // 입력값 검증 및 전처리
//            if (address == null || address.trim().isEmpty()) {
//                log.warn("잘못된 주소: {}", address);
//                return ResponseEntity.badRequest().build();
//            }
//
//            // 주소 문자열 정리 (앞뒤 따옴표 제거, 공백 정리)
//            String cleanAddress = address.trim();
//            if (cleanAddress.startsWith("\"") && cleanAddress.endsWith("\"")) {
//                cleanAddress = cleanAddress.substring(1, cleanAddress.length() - 1);
//            }
//            cleanAddress = cleanAddress.trim();
//
//            log.info("정리된 주소: {}", cleanAddress);
//
//            // 좌표 조회
//            CoordinateDTO coordinate = propertyMapService.getCoordinateFromAddress(cleanAddress);
//
//            if (coordinate != null) {
//                log.info("주소 좌표 변환 성공: {}", cleanAddress);
//                return ResponseEntity.ok(coordinate);
//            } else {
//                log.warn("좌표를 찾을 수 없음: {}", cleanAddress);
//                return ResponseEntity.notFound().build();
//            }
//
//        } catch (Exception e) {
//            log.error("주소 좌표 변환 중 오류 발생: {}", e.getMessage(), e);
//            return ResponseEntity.internalServerError().build();
//        }
//    }

    /**
     * 펀딩 ID를 이용하여 최근 3개월 실거래가와 좌표 정보를 조회
     *
     * @param fundingId 펀딩 ID
     * @return 실거래가와 좌표 정보 목록
     */
    @GetMapping("/{fundingId}")
    @ApiOperation(value = "펀딩 ID 기반 실거래가 위치 정보 조회",
            notes = "펀딩 ID를 경로 변수로 받아서 해당 펀딩의 매물 법정동 코드를 조회한 후, 최근 3개월 실거래가와 좌표 정보를 제공합니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "조회 성공", response = RealEstateLocationDTO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "잘못된 펀딩 ID 또는 법정동 코드 형식 오류"),
            @ApiResponse(code = 404, message = "펀딩 또는 매물을 찾을 수 없거나 법정동 코드가 없음"),
            @ApiResponse(code = 500, message = "서버 내부 오류")
    })
    public ResponseEntity<List<RealEstateLocationDTO>> getRealEstateLocations(
            @ApiParam(value = "펀딩 ID", required = true, example = "1")
            @PathVariable("fundingId") Long fundingId) {

        try {
            log.info("최근 3개월 실거래가 위치 정보 요청 - 펀딩ID: {}", fundingId);

            // 입력값 검증
            if (fundingId == null || fundingId <= 0) {
                log.warn("잘못된 펀딩 ID: {}", fundingId);
                return ResponseEntity.badRequest().build();
            }

            // 펀딩 ID로 법정동 코드 조회
            String rawdCd = propertyService.getRawdCdByFundingId(fundingId);

            if (rawdCd == null || rawdCd.trim().isEmpty()) {
                log.warn("펀딩 ID: {}에 해당하는 법정동 코드를 찾을 수 없습니다", fundingId);
                return ResponseEntity.notFound().build();
            }

            // 법정동코드 형식 검증 (5자리 숫자)
            if (!rawdCd.matches("\\d{5}")) {
                log.warn("법정동코드 형식 오류 - 펀딩ID: {}, 법정동코드: {}", fundingId, rawdCd);
                return ResponseEntity.badRequest().build();
            }

            log.info("펀딩 ID: {}의 법정동 코드: {}", fundingId, rawdCd);

            // 실거래가 위치 정보 조회 (서비스에서 이미 50개로 제한됨)
            List<RealEstateLocationDTO> locations = propertyMapService.getRealEstateLocations(rawdCd);

            log.info("최근 3개월 실거래가 위치 정보 응답 - 펀딩ID: {}, 법정동코드: {}, 총 {}건",
                    fundingId, rawdCd, locations.size());

            return ResponseEntity.ok(locations);

        } catch (Exception e) {
            log.error("최근 3개월 실거래가 위치 정보 조회 중 오류 발생 - 펀딩ID: {}, 오류: {}", fundingId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 펀딩 ID를 이용하여 해당 매물의 좌표를 반환하는 API (S3 독립적)
     *
     * @param fundingId 펀딩 ID
     * @return 좌표 정보
     */
    @GetMapping("/coordinate/{fundingId}")
    @ApiOperation(value = "펀딩 ID 기반 좌표 조회",
            notes = "펀딩 ID를 경로 변수로 받아서 해당 펀딩의 매물 주소를 조회한 후, 위도, 경도 좌표로 변환합니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "변환 성공", response = CoordinateDTO.class),
            @ApiResponse(code = 400, message = "잘못된 펀딩 ID"),
            @ApiResponse(code = 404, message = "펀딩 또는 매물을 찾을 수 없거나 좌표 변환 실패"),
            @ApiResponse(code = 500, message = "서버 내부 오류")
    })
    public ResponseEntity<CoordinateDTO> getPropertyCoordinate(
            @ApiParam(value = "펀딩 ID", required = true, example = "1")
            @PathVariable("fundingId") Long fundingId) {

        try {
            log.info("펀딩 ID 기반 좌표 조회 요청 - 펀딩ID: {}", fundingId);

            // 입력값 검증
            if (fundingId == null || fundingId <= 0) {
                log.warn("잘못된 펀딩 ID: {}", fundingId);
                return ResponseEntity.badRequest().build();
            }

            // 펀딩 ID로 매물 주소 조회 (S3 의존성 없음)
            String address = propertyService.getPropertyAddressByFundingId(fundingId);
            if (address == null || address.trim().isEmpty()) {
                log.warn("펀딩 ID: {}에 해당하는 주소를 찾을 수 없습니다", fundingId);
                return ResponseEntity.notFound().build();
            }

            log.info("펀딩 ID로 매물 주소 조회 완료 - 펀딩ID: {}, 주소: {}", fundingId, address);

            // 좌표 조회
            CoordinateDTO coordinate = propertyMapService.getCoordinateFromAddress(address);

            if (coordinate != null) {
                log.info("펀딩 ID로 매물 좌표 조회 성공 - 펀딩ID: {}, 위도: {}, 경도: {}",
                        fundingId, coordinate.getLatitude(), coordinate.getLongitude());
                return ResponseEntity.ok(coordinate);
            } else {
                log.warn("펀딩 ID로 매물 주소의 좌표를 찾을 수 없음 - 펀딩ID: {}, 주소: {}", fundingId, address);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("펀딩 ID로 매물 좌표 조회 중 오류 발생 - 펀딩ID: {}, 오류: {}", fundingId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 주소를 직접 받아서 좌표를 반환하는 API
     *
     * @param address 주소
     * @return 좌표 정보
     */
    @PostMapping("/coordinate")
    @ApiOperation(value = "주소 기반 좌표 변환",
            notes = "주소를 직접 받아서 위도, 경도 좌표로 변환합니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "변환 성공", response = CoordinateDTO.class),
            @ApiResponse(code = 400, message = "잘못된 주소 형식"),
            @ApiResponse(code = 404, message = "주소에 해당하는 좌표를 찾을 수 없음"),
            @ApiResponse(code = 500, message = "서버 내부 오류")
    })
    public ResponseEntity<CoordinateDTO> getCoordinateFromAddress(
            @ApiParam(value = "변환할 주소", required = true, example = "서울특별시 강남구 테헤란로 427")
            @RequestBody String address) {

        try {
            log.info("주소 기반 좌표 변환 요청: {}", address);

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
                log.info("주소 기반 좌표 변환 성공: {}", cleanAddress);
                return ResponseEntity.ok(coordinate);
            } else {
                log.warn("좌표를 찾을 수 없음: {}", cleanAddress);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("주소 기반 좌표 변환 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 헬스체크 엔드포인트
     *
     * @return 상태 정보
     */
    @GetMapping("/health")
    @ApiOperation(value = "헬스체크", notes = "API 서버 상태를 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "서버 정상 작동", response = String.class)
    })
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Property Map API is running");
    }

    /**
     * [DEPRECATED] 법정동코드를 직접 받는 기존 API (하위 호환성 유지)
     *
     * @param rawdCd 법정동코드 (5자리)
     * @return 실거래가와 좌표 정보 목록
     */
//    @GetMapping("/real-estate-locations/by-code")
//    @ApiOperation(value = "[DEPRECATED] 법정동코드 직접 입력 방식",
//                  notes = "⚠️ 사용 중단 예정: 법정동코드를 직접 받아서 실거래가 정보를 조회합니다. 대신 '/real-estate-locations?propertyId=' 사용을 권장합니다.")
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "조회 성공", response = RealEstateLocationDTO.class, responseContainer = "List"),
//            @ApiResponse(code = 400, message = "잘못된 법정동코드 형식 (5자리 숫자 필요)"),
//            @ApiResponse(code = 500, message = "서버 내부 오류")
//    })
//    public ResponseEntity<List<RealEstateLocationDTO>> getRealEstateLocationsByCode(
//            @ApiParam(value = "법정동코드 (5자리 숫자)", required = true, example = "11680")
//            @RequestParam("rawdCd") String rawdCd) {
//
//        try {
//            log.info("최근 3개월 실거래가 위치 정보 요청 (법정동코드 직접) - 법정동코드: {}", rawdCd);
//
//            // 입력값 검증
//            if (rawdCd == null || rawdCd.trim().isEmpty()) {
//                log.warn("잘못된 법정동코드: {}", rawdCd);
//                return ResponseEntity.badRequest().build();
//            }
//
//            // 법정동코드 형식 검증 (5자리 숫자)
//            if (!rawdCd.matches("\\d{5}")) {
//                log.warn("법정동코드 형식 오류: {}", rawdCd);
//                return ResponseEntity.badRequest().build();
//            }
//
//            // 실거래가 위치 정보 조회
//            List<RealEstateLocationDTO> locations = propertyMapService.getRealEstateLocations(rawdCd);
//
//            log.info("최근 3개월 실거래가 위치 정보 응답 (법정동코드 직접) - 총 {}건", locations.size());
//
//            return ResponseEntity.ok(locations);
//
//        } catch (Exception e) {
//            log.error("최근 3개월 실거래가 위치 정보 조회 중 오류 발생 (법정동코드 직접): {}", e.getMessage(), e);
//            return ResponseEntity.internalServerError().build();
//        }
//    }

    /**
     * 매물 ID로 법정동 코드 조회 API (테스트용)
     *
     * @param propertyId 매물 ID
     * @return 법정동 코드
     */
//    @GetMapping("/property/{propertyId}/rawd-cd")
//    @ApiOperation(value = "매물 ID로 법정동 코드 조회",
//                  notes = "매물 ID를 받아서 해당 매물의 법정동 코드를 조회합니다. (테스트 및 확인용)")
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "조회 성공", response = String.class),
//            @ApiResponse(code = 400, message = "잘못된 매물 ID"),
//            @ApiResponse(code = 404, message = "매물을 찾을 수 없거나 법정동 코드가 없음"),
//            @ApiResponse(code = 500, message = "서버 내부 오류")
//    })
//    public ResponseEntity<String> getRawdCdByPropertyId(
//            @ApiParam(value = "매물 ID", required = true, example = "1")
//            @PathVariable Long propertyId) {
//        try {
//            log.info("매물 ID로 법정동 코드 조회 요청 - 매물ID: {}", propertyId);
//
//            if (propertyId == null || propertyId <= 0) {
//                log.warn("잘못된 매물 ID: {}", propertyId);
//                return ResponseEntity.badRequest().build();
//            }
//
//            String rawdCd = propertyService.getRawdCdByPropertyId(propertyId);
//
//            if (rawdCd == null || rawdCd.trim().isEmpty()) {
//                log.warn("매물 ID: {}에 해당하는 법정동 코드를 찾을 수 없습니다", propertyId);
//                return ResponseEntity.notFound().build();
//            }
//
//            log.info("법정동 코드 조회 성공 - 매물ID: {}, 법정동코드: {}", propertyId, rawdCd);
//            return ResponseEntity.ok(rawdCd);
//
//        } catch (Exception e) {
//            log.error("법정동 코드 조회 중 오류 발생 - 매물ID: {}, 오류: {}", propertyId, e.getMessage(), e);
//            return ResponseEntity.internalServerError().build();
//        }
//    }
}
