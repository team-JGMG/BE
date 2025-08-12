package org.bobj.funding.controller;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.bobj.common.dto.CustomSlice;
import org.bobj.common.exception.ErrorResponse;
import org.bobj.common.response.ApiCommonResponse;
import org.bobj.funding.dto.FundingDetailResponseDTO;
import org.bobj.funding.dto.FundingEndedResponseDTO;
import org.bobj.funding.dto.FundingTotalResponseDTO;
import org.bobj.funding.service.FundingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/funding")
@RequiredArgsConstructor
@Slf4j
@Api(tags="펀딩 API")
public class FundingController {
    private final FundingService fundingService;
    private final org.bobj.common.crypto.DecryptionResponseAdvice decryptionResponseAdvice;

    @GetMapping("/{fundingId}")
    @ApiOperation(value = "펀딩 상세 조회", notes = "특정 펀딩에 관련된 매물 정보를 조회합니다.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "fundingId", value = "조회할 펀딩 ID", required = true, dataType = "long", paramType = "path")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "펀딩 상세 조회 성공", response = FundingDetailResponseDTO.class),
            @ApiResponse(code = 400, message = "잘못된 요청 (예: 유효하지 않은 펀딩 ID)", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<FundingDetailResponseDTO>> getFundingDetail(
            @PathVariable @ApiParam(value = "펀딩 ID", required = true) Long fundingId) {
        FundingDetailResponseDTO detail = fundingService.getFundingDetail(fundingId);
        FundingDetailResponseDTO decryptedDetail = decryptionResponseAdvice.decryptFundingDetailResponseDTO(detail);

        return ResponseEntity.ok(ApiCommonResponse.createSuccess(decryptedDetail));
    }

    @GetMapping
    @ApiOperation(value= "펀딩 목록 조회", notes = "[펀딩 모집 페이지]카테고리 필터, 정렬 필터에 따른 펀딩 목록을 조회합니다.(무한스크롤 구현)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "category", value = "카테고리 필터명(funding -> 모집중 ,ended -> 펀딩 완료 ,sold -> 매각 완료)", defaultValue = "funding", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "sort", value = "정렬 필터명(timeLeft -> 남은 시간 ,rate -> 모집률, 디폴트는 등록일순)", defaultValue = "date" ,dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "page", value = "페이지 번호 (0부터 시작)", defaultValue = "0" ,dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "size", value = "한 페이지당 항목 수", defaultValue = "10", dataType = "int", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "펀딩 목록 조회 성공", response = FundingTotalResponseDTO.class, responseContainer = "CustomSlice"),
            @ApiResponse(code = 400, message = "잘못된 요청", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<CustomSlice<FundingTotalResponseDTO>>> getFundingList(
            @RequestParam(value = "category", defaultValue = "funding") String category,
            @RequestParam(value = "sort", defaultValue = "date") String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        CustomSlice<FundingTotalResponseDTO> detail = fundingService.getFundingList(category, sort, page, size);
        return ResponseEntity.ok(ApiCommonResponse.createSuccess(detail));
    }

    @GetMapping("/ended")
    @ApiOperation(value="성공된 펀딩 목록 조회" , notes = "[매물 거래 페이지]성공한 펀딩 목록을 조회합니다. (무한 스크롤 구현)")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "페이지 번호 (0부터 시작)", defaultValue = "0" ,dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "size", value = "한 페이지당 항목 수", defaultValue = "10", dataType = "int", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "성공된 목록 조회 성공", response = FundingEndedResponseDTO.class, responseContainer = "CustomSlice"),
            @ApiResponse(code = 400, message = "잘못된 요청", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<CustomSlice<FundingEndedResponseDTO>>> getEndedFundingProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        CustomSlice<FundingEndedResponseDTO> detail = fundingService.getEndedFundingProperties(page, size);
        return ResponseEntity.ok(ApiCommonResponse.createSuccess(detail));
    }



    @GetMapping("/expire")
    public String testExpireFunding() {
        try {
            fundingService.expireFunding();
            return "expireFunding() 실행 완료";
        } catch (Exception e) {
            log.error("expireFunding 테스트 실패", e);
            return "expireFunding() 실행 중 오류 발생: " + e.getMessage();
        }
    }
}
