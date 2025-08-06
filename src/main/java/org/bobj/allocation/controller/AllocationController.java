package org.bobj.allocation.controller;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.allocation.dto.AllocationResponseDTO;
import org.bobj.allocation.service.AllocationBatchService;
import org.bobj.allocation.service.AllocationService;
import org.bobj.common.exception.ErrorResponse;
import org.bobj.common.response.ApiCommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/allocations")
@RequiredArgsConstructor
@Api(tags = "배당금 관리 API", description = "펀딩 배당금 조회 및 지급 관리")
public class AllocationController {

    private final AllocationService allocationService;
    private final AllocationBatchService allocationBatchService;

    /**
     * 펀딩별 배당금 내역 조회
     */
    @GetMapping("/{fundingId}")
    @ApiOperation(value = "펀딩 배당금 내역 조회", 
                 notes = "특정 펀딩의 모든 배당금 내역을 조회합니다. 최신순으로 정렬되어 반환됩니다.\n\n" +
                        "**배당금 계산 방식:**\n" +
                        "- 총 배당금: 임대수익의 90% (10% 수수료 제외)\n" +
                        "- 주당 배당금: 총 배당금 ÷ 총 주식 수\n" +
                        "- 개인 배당금: 주당 배당금 × 개인 보유 주식 수")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "배당금 내역 조회 성공\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": \"success\",\n" +
                    "  \"data\": [\n" +
                    "    {\n" +
                    "      \"allocationsId\": 1,\n" +
                    "      \"fundingId\": 123,\n" +
                    "      \"propertyTitle\": \"강남 오피스텔\",\n" +
                    "      \"dividendPerShare\": 1350.00,\n" +
                    "      \"totalDividendAmount\": 135000.00,\n" +
                    "      \"totalShares\": 100,\n" +
                    "      \"paymentDate\": \"2025-09-06\",\n" +
                    "      \"paymentStatus\": \"COMPLETED\",\n" +
                    "      \"paymentStatusKorean\": \"지급완료\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"allocationsId\": 2,\n" +
                    "      \"fundingId\": 123,\n" +
                    "      \"propertyTitle\": \"강남 오피스텔\",\n" +
                    "      \"dividendPerShare\": 1350.00,\n" +
                    "      \"totalDividendAmount\": 135000.00,\n" +
                    "      \"totalShares\": 100,\n" +
                    "      \"paymentDate\": \"2025-10-06\",\n" +
                    "      \"paymentStatus\": \"PENDING\",\n" +
                    "      \"paymentStatusKorean\": \"지급예정\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n" +
                    "```"),
            @ApiResponse(code = 400, message = "잘못된 펀딩 ID\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": \"error\",\n" +
                    "  \"message\": \"배당금 내역 조회에 실패했습니다.\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "펀딩을 찾을 수 없음\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": \"error\",\n" +
                    "  \"message\": \"배당금 내역 조회에 실패했습니다.\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": \"error\",\n" +
                    "  \"message\": \"배당금 내역 조회에 실패했습니다.\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<List<AllocationResponseDTO>>> getAllocationsByFundingId(
            @ApiParam(value = "조회할 펀딩 ID", required = true, example = "123")
            @PathVariable("fundingId") Long fundingId) {
        
        log.info("펀딩 배당금 내역 조회 요청 - 펀딩 ID: {}", fundingId);

        try {
            List<AllocationResponseDTO> allocations = allocationService.getAllocationsByFundingId(fundingId);
            log.info("펀딩 배당금 내역 조회 성공 - 펀딩 ID: {}, 배당금 내역 수: {}", fundingId, allocations.size());
            return ResponseEntity.ok(ApiCommonResponse.createSuccess(allocations));

        } catch (Exception e) {
            log.error("펀딩 배당금 내역 조회 실패 - 펀딩 ID: {}", fundingId, e);
            return ResponseEntity.internalServerError().body(
                (ApiCommonResponse<List<AllocationResponseDTO>>) ApiCommonResponse.createError("배당금 내역 조회에 실패했습니다.")
            );
        }
    }

    /**
     * 배당금 지급 처리 (관리자용)
     */
    @PostMapping("/process-payment")
    @ApiOperation(value = "배당금 지급 처리 (관리자용)", 
                 notes = "오늘 날짜에 지급 예정인 배당금을 처리하고 다음 배당을 생성합니다.\n\n" +
                        "**처리 과정:**\n" +
                        "1. 오늘 지급 예정인 PENDING 상태 배당금 조회\n" +
                        "2. 각 펀딩의 주식 보유자들에게 배당금 지급 (포인트 증가)\n" +
                        "3. 배당금 상태를 COMPLETED로 변경\n" +
                        "4. 다음 달 배당금 자동 생성\n\n" +
                        "**주의:** 관리자 권한이 필요한 기능입니다.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "배당금 지급 처리 성공\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": \"success\",\n" +
                    "  \"data\": \"배당금 지급 처리가 완료되었습니다.\"\n" +
                    "}\n" +
                    "```"),
            @ApiResponse(code = 401, message = "인증 필요\n\n" +
                    "**예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"status\": \"error\",\n" +
                    "  \"message\": \"배당금 지급 처리에 실패했습니다.\"\n" +
                    "}\n" +
                    "```", response = ErrorResponse.class),
            @ApiResponse(code = 403, message = "관리자 권한 필요", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<String>> processPayment() {
        
        log.info("배당금 지급 처리 요청");

        try {
            allocationService.processPaymentAndCreateNext(java.time.LocalDate.now());
            log.info("배당금 지급 처리 성공");
            return ResponseEntity.ok(ApiCommonResponse.createSuccess("배당금 지급 처리가 완료되었습니다."));

        } catch (Exception e) {
            log.error("배당금 지급 처리 실패", e);
            return ResponseEntity.internalServerError().body(
                (ApiCommonResponse<String>) ApiCommonResponse.createError("배당금 지급 처리에 실패했습니다.")
            );
        }
    }
//
//    /**
//     * 첫 배당금 생성 (관리자용)
//     */
//    @PostMapping("/create-first/{fundingId}")
//    @ApiOperation(value = "첫 배당금 생성 (관리자용)",
//                 notes = "펀딩 완료 시 첫 번째 배당금을 생성합니다.\n\n" +
//                        "**생성 과정:**\n" +
//                        "1. 펀딩의 임대수익과 총 주식 수 조회\n" +
//                        "2. 배당금 계산 (임대수익의 90%)\n" +
//                        "3. 주당 배당금 계산 (총 배당금 ÷ 총 주식 수)\n" +
//                        "4. 한 달 후 지급일로 배당금 레코드 생성\n\n" +
//                        "**참고:** 일반적으로 펀딩 완료 시 자동으로 실행되므로 수동 호출은 드뭅니다.")
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "첫 배당금 생성 성공\n\n" +
//                    "**예시:**\n" +
//                    "```json\n" +
//                    "{\n" +
//                    "  \"status\": \"SUCCESS\",\n" +
//                    "  \"data\": \"첫 배당금 생성이 완료되었습니다.\"\n" +
//                    "}\n" +
//                    "```"),
//            @ApiResponse(code = 400, message = "잘못된 펀딩 ID 또는 데이터\n\n" +
//                    "**예시:**\n" +
//                    "```json\n" +
//                    "{\n" +
//                    "  \"status\": 400,\n" +
//                    "  \"code\": \"ALLOCATION007\",\n" +
//                    "  \"message\": \"펀딩 정보가 유효하지 않거나 이미 배당금이 생성되었습니다.\",\n" +
//                    "  \"path\": \"/api/allocations/create-first/{fundingId}\"\n" +
//                    "}\n" +
//                    "```", response = ErrorResponse.class),
//            @ApiResponse(code = 401, message = "인증 필요\n\n" +
//                    "**예시:**\n" +
//                    "```json\n" +
//                    "{\n" +
//                    "  \"status\": 401,\n" +
//                    "  \"code\": \"ALLOCATION008\",\n" +
//                    "  \"message\": \"인증이 필요합니다. 관리자 로그인 후 다시 시도해주세요.\",\n" +
//                    "  \"path\": \"/api/allocations/create-first/{fundingId}\"\n" +
//                    "}\n" +
//                    "```", response = ErrorResponse.class),
//            @ApiResponse(code = 403, message = "관리자 권한 필요\n\n" +
//                    "**예시:**\n" +
//                    "```json\n" +
//                    "{\n" +
//                    "  \"status\": 403,\n" +
//                    "  \"code\": \"ALLOCATION009\",\n" +
//                    "  \"message\": \"관리자 권한이 필요합니다.\",\n" +
//                    "  \"path\": \"/api/allocations/create-first/{fundingId}\"\n" +
//                    "}\n" +
//                    "```", response = ErrorResponse.class),
//            @ApiResponse(code = 404, message = "펀딩을 찾을 수 없음\n\n" +
//                    "**예시:**\n" +
//                    "```json\n" +
//                    "{\n" +
//                    "  \"status\": 404,\n" +
//                    "  \"code\": \"ALLOCATION010\",\n" +
//                    "  \"message\": \"해당 펀딩을 찾을 수 없습니다.\",\n" +
//                    "  \"path\": \"/api/allocations/create-first/{fundingId}\"\n" +
//                    "}\n" +
//                    "```", response = ErrorResponse.class),
//            @ApiResponse(code = 500, message = "서버 내부 오류\n\n" +
//                    "**예시:**\n" +
//                    "```json\n" +
//                    "{\n" +
//                    "  \"status\": 500,\n" +
//                    "  \"code\": \"ALLOCATION011\",\n" +
//                    "  \"message\": \"첫 배당금 생성 중 서버 오류가 발생했습니다.\",\n" +
//                    "  \"path\": \"/api/allocations/create-first/{fundingId}\"\n" +
//                    "}\n" +
//                    "```", response = ErrorResponse.class)
//    })
//    public ResponseEntity<ApiCommonResponse<String>> createFirstAllocation(
//            @ApiParam(value = "배당금을 생성할 펀딩 ID", required = true, example = "123")
//            @PathVariable("fundingId") Long fundingId) {
//
//        log.info("첫 배당금 생성 요청 - 펀딩 ID: {}", fundingId);
//
//        try {
//            allocationService.createFirstAllocation(fundingId);
//            log.info("첫 배당금 생성 성공 - 펀딩 ID: {}", fundingId);
//            return ResponseEntity.ok(ApiCommonResponse.createSuccess("첫 배당금 생성이 완료되었습니다."));
//
//        } catch (Exception e) {
//            log.error("첫 배당금 생성 실패 - 펀딩 ID: {}", fundingId, e);
//            return ResponseEntity.internalServerError().body(
//                (ApiCommonResponse<String>) ApiCommonResponse.createError("첫 배당금 생성에 실패했습니다.")
//            );
//        }
//    }
//
//    /**
//     * 대량 배당금 지급 처리 (관리자용 - 멀티스레드)
//     */
//    @PostMapping("/process-batch-payment")
//    @ApiOperation(value = "대량 배당금 지급 처리 (관리자용 - 멀티스레드)",
//                 notes = "대량의 배당금을 멀티스레드로 안전하게 처리합니다.\n\n" +
//                        "**처리 방식:**\n" +
//                        "- 10만명 기준으로 1,000명씩 청크 단위로 분할\n" +
//                        "- 10개 스레드 풀에서 동시 처리\n" +
//                        "- 개별 실패가 전체를 망가뜨리지 않음\n" +
//                        "- 상세한 처리 결과 로깅\n\n" +
//                        "**주의:** 대량 처리로 인해 응답 시간이 길 수 있습니다.")
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "대량 배당금 지급 처리 성공\n\n" +
//                    "**예시:**\n" +
//                    "```json\n" +
//                    "{\n" +
//                    "  \"status\": \"SUCCESS\",\n" +
//                    "  \"data\": \"대량 배당금 지급 배치 처리가 완료되었습니다.\"\n" +
//                    "}\n" +
//                    "```"),
//            @ApiResponse(code = 401, message = "인증 필요\n\n" +
//                    "**예시:**\n" +
//                    "```json\n" +
//                    "{\n" +
//                    "  \"status\": 401,\n" +
//                    "  \"code\": \"ALLOCATION012\",\n" +
//                    "  \"message\": \"인증이 필요합니다. 관리자 로그인 후 다시 시도해주세요.\",\n" +
//                    "  \"path\": \"/api/allocations/process-batch-payment\"\n" +
//                    "}\n" +
//                    "```", response = ErrorResponse.class),
//            @ApiResponse(code = 403, message = "관리자 권한 필요\n\n" +
//                    "**예시:**\n" +
//                    "```json\n" +
//                    "{\n" +
//                    "  \"status\": 403,\n" +
//                    "  \"code\": \"ALLOCATION013\",\n" +
//                    "  \"message\": \"관리자 권한이 필요합니다.\",\n" +
//                    "  \"path\": \"/api/allocations/process-batch-payment\"\n" +
//                    "}\n" +
//                    "```", response = ErrorResponse.class),
//            @ApiResponse(code = 500, message = "서버 내부 오류\n\n" +
//                    "**예시:**\n" +
//                    "```json\n" +
//                    "{\n" +
//                    "  \"status\": 500,\n" +
//                    "  \"code\": \"ALLOCATION014\",\n" +
//                    "  \"message\": \"대량 배당금 지급 처리 중 서버 오류가 발생했습니다.\",\n" +
//                    "  \"path\": \"/api/allocations/process-batch-payment\"\n" +
//                    "}\n" +
//                    "```", response = ErrorResponse.class),
//            @ApiResponse(code = 503, message = "서버 과부하\n\n" +
//                    "**예시:**\n" +
//                    "```json\n" +
//                    "{\n" +
//                    "  \"status\": 503,\n" +
//                    "  \"code\": \"ALLOCATION015\",\n" +
//                    "  \"message\": \"서버가 과부하 상태입니다. 잠시 후 다시 시도해주세요.\",\n" +
//                    "  \"path\": \"/api/allocations/process-batch-payment\"\n" +
//                    "}\n" +
//                    "```", response = ErrorResponse.class)
//    })
//    public ResponseEntity<ApiCommonResponse<String>> processBatchPayment() {
//
//        log.info("대량 배당금 지급 배치 처리 요청");
//
//        try {
//            allocationBatchService.processBatchAllocationPayments(java.time.LocalDate.now());
//            log.info("대량 배당금 지급 배치 처리 성공");
//            return ResponseEntity.ok(ApiCommonResponse.createSuccess("대량 배당금 지급 배치 처리가 완료되었습니다."));
//
//        } catch (Exception e) {
//            log.error("대량 배당금 지급 배치 처리 실패", e);
//            return ResponseEntity.internalServerError().body(
//                    (ApiCommonResponse<String>) ApiCommonResponse.createError("대량 배당금 지급 배치 처리에 실패했습니다.")
//            );
//        }
//    }
}
