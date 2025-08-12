package org.bobj.property.controller;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.common.crypto.DecryptionResponseAdvice;
import org.bobj.common.dto.CustomSlice;
import org.bobj.common.exception.ErrorResponse;
import org.bobj.common.response.ApiCommonResponse;
import org.bobj.property.domain.PropertyDocumentType;
import org.bobj.property.dto.*;
import org.bobj.property.service.PropertyService;
import org.bobj.user.security.UserPrincipal;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Log4j2
@Api(tags="매물 API")
public class PropertyController {
    private final PropertyService propertyService;
    private final DecryptionResponseAdvice decryptionResponseAdvice;

    @PostMapping(
            value = "/auth/property",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(value = "매물 등록", notes = "새로운 매물을 등록합니다. 법정동코드가 있으면 자동으로 전월세 데이터를 조회하여 월세 정보를 설정합니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "매물 등록 성공"),
            @ApiResponse(code = 400, message = "잘못된 요청", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<Void>> createProperty(
            @ApiIgnore @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("request") PropertyCreateDTO requestDTO,
            @RequestPart(value = "photoFiles", required = false) List<MultipartFile> photoFiles,
            @RequestPart(value = "documentFiles", required = false) List<MultipartFile> documentFiles,
            @RequestPart(value = "documentTypes", required = false) List<String> documentTypes) {
        // documentFiles + documentTypes 수동 매핑
        List<PropertyDocumentRequestDTO> documentRequests = new ArrayList<>();
        if (documentFiles != null && documentTypes != null) {
            for (int i = 0; i < documentFiles.size(); i++) {
                documentRequests.add(new PropertyDocumentRequestDTO(
                        documentFiles.get(i),
                        PropertyDocumentType.valueOf(documentTypes.get(i)) // 예외처리 필수
                ));
            }
        }
        Long userId = principal.getUserId();
        propertyService.registerProperty(userId,requestDTO, photoFiles, documentRequests);
        return ResponseEntity.ok(ApiCommonResponse.createSuccess(null));
    }


    @GetMapping("/auth/property")
    @ApiOperation(value = "매물 목록 조회", notes = "[관리자] 요약 정보가 담긴 매물 목록을 반환합니다.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "category", value = "카테고리 필터명(pending -> 대기중 ,approved -> 승인 ,failed -> 펀딩 실패)", defaultValue = "pending", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "page", value = "페이지 번호 (0부터 시작)", defaultValue = "0" ,dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "size", value = "한 페이지당 항목 수", defaultValue = "10", dataType = "int", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "매물 목록 조회 성공", response = PropertyTotalDTO.class, responseContainer = "CustomSlice"),
            @ApiResponse(code = 400, message = "잘못된 요청", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<CustomSlice<PropertyTotalDTO>>> getPropertiesByStatus(
            @RequestParam(value = "category", defaultValue = "funding") String category,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        CustomSlice<PropertyTotalDTO> list = propertyService.getAllPropertiesByStatus(category, page, size);
        return ResponseEntity.ok(ApiCommonResponse.createSuccess(list));
    }

    @GetMapping("/auth/property/user/{userId}")
    @ApiOperation(value = "사용자 매물 목록 조회", notes = "[매도자] 특정 사용자가 등록한 매물 목록을 조회합니다.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer 토큰", required = true, dataTypeClass = String.class, paramType = "header"),
            @ApiImplicitParam(name = "status", value = "매물 상태(pending -> 대기중, approved -> 승인, rejected -> 거절됨, sold -> 매각", defaultValue = "pending" ,dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "page", value = "페이지 번호 (0부터 시작)", defaultValue = "0" ,dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "size", value = "한 페이지당 항목 수", defaultValue = "10", dataType = "int", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "사용자 매물 목록 조회 성공", response = PropertyUserResponseDTO.class, responseContainer = "CustomSlice"),
            @ApiResponse(code = 400, message = "잘못된 요청", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<CustomSlice<PropertyUserResponseDTO>>> getUserPropertiesByStatus(
            @ApiIgnore @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = principal.getUserId();
        CustomSlice<PropertyUserResponseDTO> response =
                propertyService.getUserPropertiesByStatus(userId, status, page, size);
        return ResponseEntity.ok(ApiCommonResponse.createSuccess(response));
    }

    @GetMapping("/auth/property/{propertyId}")
    @ApiOperation(value = "매물 상세 조회", notes = "[관리자, 매도자]특정 매물의 상세 정보를 반환합니다.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "propertyId", value = "매물 ID", required = true, dataType = "long", paramType = "path")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "매물 상세 조회 성공", response = PropertyDetailDTO.class),
            @ApiResponse(code = 400, message = "잘못된 요청", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<PropertyDetailDTO>> getPropertyById(
            @PathVariable @ApiParam(value = "매물 ID", required = true) Long propertyId) {
        PropertyDetailDTO result = propertyService.getPropertyById(propertyId);
        
        PropertyDetailDTO decryptedResult = decryptionResponseAdvice.decryptPropertyDetailDTO(result);
        return ResponseEntity.ok(ApiCommonResponse.createSuccess(decryptedResult));
    }

    @PatchMapping("/auth/property/{propertyId}/status")
    @ApiOperation(value = "매물 상태 변경", notes = "매물을 승인 또는 거절합니다.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "propertyId", value = "매물 ID", required = true, dataType = "long", paramType = "path"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "매물 상태 변경 성공"),
            @ApiResponse(code = 400, message = "잘못된 요청", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<String>> updatePropertyStatus(
            @PathVariable @ApiParam(value = "펀딩 ID", required = true) Long propertyId,
            @RequestBody @ApiParam(value = "상태 정보", required = true)
            PropertyStatusUpdateRequestDTO request) {

        propertyService.updatePropertyStatus(propertyId, request.getStatus());
        return ResponseEntity.ok(ApiCommonResponse.createSuccess("매물 상태 변경 완료"));
    }

    @GetMapping("/property/sold")
    @ApiOperation(value = "매각 완료 매물 목록 조회", notes = "[메인] 매각 완료 매물들을 불러옵니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "매각 완료 매물 목록 조회 성공", response = PropertySoldResponseDTO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "잘못된 요청", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<List<PropertySoldResponseDTO>>> getSoldProperties() {
        List<PropertySoldResponseDTO> response = propertyService.getSoldProperties();
        return ResponseEntity.ok(ApiCommonResponse.createSuccess(response));
    }

    // =========================== 테스트용 전월세 API ===========================
//
//    @GetMapping("/test/rental-calculation")
//    @ApiOperation(value = "[테스트용] 전월세 자동 계산 테스트", notes = "법정동코드와 주소를 입력하여 자동 월세 계산 로직을 테스트합니다.")
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "lawd_cd", value = "법정동코드 (5자리)", required = true, dataType = "string", paramType = "query", example = "11110"),
//            @ApiImplicitParam(name = "address", value = "매물 주소", required = true, dataType = "string", paramType = "query", example = "서울 종로구 청운동 123 현대아파트")
//    })
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "자동 월세 계산 테스트 성공"),
//            @ApiResponse(code = 400, message = "잘못된 요청", response = ErrorResponse.class),
//            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
//    })
//    public ResponseEntity<ApiCommonResponse<String>> testRentalCalculation(
//            @RequestParam @ApiParam(value = "법정동코드 (5자리)", required = true, example = "11110") String lawd_cd,
//            @RequestParam @ApiParam(value = "매물 주소", required = true, example = "서울 종로구 청운동 123 현대아파트") String address) {
//
//        log.info("전월세 자동 계산 테스트 시작 - 법정동코드: {}, 주소: {}", lawd_cd, address);
//
//        java.math.BigDecimal result = propertyService.testRentalCalculation(lawd_cd, address);
//
//        String message;
//        if (result != null) {
//            message = String.format("자동 계산된 월세: %s원 (매칭 성공)", result.toString());
//        } else {
//            message = "매칭되는 월세 데이터를 찾을 수 없습니다.";
//        }
//
//        return ResponseEntity.ok(ApiCommonResponse.createSuccess(message));
//    }


    //매각 처리 테스트용 api
    @PostMapping("/property/sold-process")
    public ResponseEntity<String> processSoldProperties() {
        log.info("매각 처리 로직을 수동으로 시작합니다.");
        try {
            propertyService.soldProperties();
            log.info("매각 처리 로직이 성공적으로 완료되었습니다.");
            return ResponseEntity.ok("매각 처리 로직이 성공적으로 호출되었습니다.");
        } catch (Exception e) {
            log.error("매각 처리 중 오류가 발생했습니다.", e);
            return ResponseEntity.status(500).body("매각 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
