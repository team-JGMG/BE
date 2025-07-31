package org.bobj.point.controller;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.common.exception.ErrorResponse;
import org.bobj.common.response.ApiCommonResponse;
import org.bobj.payment.dto.VerifyRequestDto;
import org.bobj.payment.service.PaymentService;
import org.bobj.point.domain.PointChargeRequestVO;
import org.bobj.point.service.PointChargeRequestService;
import org.bobj.point.util.MerchantUidGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;

@RestController
@RequestMapping("/api/point")
@RequiredArgsConstructor
@Log4j2
@Api(tags = "포인트 충전 및 검증 API")
public class PointChargeRequestController {

    private final PointChargeRequestService pointChargeRequestService;
    private final PaymentService paymentService;

//    @PostMapping("/charge")
//    @ApiOperation(value = "포인트 충전 요청", notes = "사용자가 지정한 금액으로 포인트 충전 요청을 생성합니다.")
//    @ApiResponses(value = {
//        @ApiResponse(code = 200, message = "충전 요청 성공", response = ApiCommonResponse.class),
//        @ApiResponse(code = 400, message = "잘못된 요청 (금액 누락 등)", response = ErrorResponse.class),
//        @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
//    })
//    public ResponseEntity<ApiCommonResponse<String>> createCharge(
//        @RequestParam @ApiParam(value = "충전 금액", example = "5000", required = true) BigDecimal amount,
//        Principal principal
//    ) {
//        Long userId = Long.parseLong(principal.getName());
//        String merchantUid = MerchantUidGenerator.generate(userId);
//
//        PointChargeRequestVO request = PointChargeRequestVO.builder()
//            .userId(userId)
//            .amount(amount)
//            .merchantUid(merchantUid)
//            .status("PENDING")
//            .build();
//
//        pointChargeRequestService.createChargeRequest(request);
//        log.info("포인트 충전 요청 생성: userId={}, amount={}, merchantUid={}", userId, amount, merchantUid);
//
//        return ResponseEntity.ok(ApiCommonResponse.createSuccess(merchantUid));
//    }

    @PostMapping("/charge")
    @ApiOperation(value = "포인트 충전 요청", notes = "사용자가 지정한 금액으로 포인트 충전 요청을 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "충전 요청 성공", response = ApiCommonResponse.class),
        @ApiResponse(code = 400, message = "잘못된 요청 (금액 누락 등)", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<String>> createCharge(
        @RequestParam @ApiParam(value = "충전 금액", example = "5000", required = true) BigDecimal amount
        // Principal principal ← 제거
    ) {
        // TODO: 실제 배포 시 제거
        Long userId = 1L; // 임시 테스트용 사용자 ID

        String merchantUid = MerchantUidGenerator.generate(userId);

        PointChargeRequestVO request = PointChargeRequestVO.builder()
            .userId(userId)
            .amount(amount)
            .merchantUid(merchantUid)
            .status("PENDING")
            .build();

        pointChargeRequestService.createChargeRequest(request);
        log.info("포인트 충전 요청 생성: userId={}, amount={}, merchantUid={}", userId, amount, merchantUid);

        return ResponseEntity.ok(ApiCommonResponse.createSuccess(merchantUid));
    }












//    @PostMapping("/verify")
//    @ApiOperation(value = "결제 검증 및 포인트 지급", notes = "imp_uid를 통해 결제를 검증하고, 결제가 성공한 경우 포인트를 지급합니다.")
//    @ApiResponses(value = {
//        @ApiResponse(code = 200, message = "포인트 충전 성공", response = ApiCommonResponse.class),
//        @ApiResponse(code = 400, message = "잘못된 요청 (금액 불일치, 결제 실패, 중복 처리 등)\n\n" +
//            "**예시:**\n" +
//            "```json\n" +
//            "{\n" +
//            "  \"status\": 400,\n" +
//            "  \"code\": \"P001\",\n" +
//            "  \"message\": \"결제 금액이 일치하지 않습니다.\",\n" +
//            "  \"path\": \"/api/point/verify\"\n" +
//            "}\n" +
//            "```",
//            response = ErrorResponse.class),
//        @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
//    })
//    public ResponseEntity<ApiCommonResponse<String>> verifyPayment(
//        @RequestBody @ApiParam(value = "결제 검증 요청 DTO", required = true)
//        VerifyRequestDto requestDto,
//        Principal principal
//    ) {
//        Long userId = Long.parseLong(principal.getName());
//        log.info("결제 검증 요청: userId={}, impUid={}", userId, requestDto.getImpUid());
//
//        paymentService.verifyPayment(userId, requestDto);
//
//        return ResponseEntity.ok(ApiCommonResponse.createSuccess("포인트 충전 성공"));
//    }
//}

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody VerifyRequestDto dto) {
        Long testUserId = 1L; // 실제 로그인 구현 전 테스트용 하드코딩

        paymentService.verifyPayment(testUserId, dto);

        return ResponseEntity.ok(ApiCommonResponse.createSuccess("포인트 충전 성공"));
    }


    public ResponseEntity<ApiCommonResponse<String>> verifyPayment(
        @RequestBody @ApiParam(value = "결제 검증 요청 DTO", required = true)
        VerifyRequestDto requestDto,
        Principal principal
    ) {
        Long userId = Long.parseLong(principal.getName());
        log.info("결제 검증 요청: userId={}, impUid={}", userId, requestDto.getImpUid());

        paymentService.verifyPayment(userId, requestDto);

        return ResponseEntity.ok(ApiCommonResponse.createSuccess("포인트 충전 성공"));
    }
}
//package org.bobj.point.controller;
//
//import io.swagger.annotations.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.log4j.Log4j2;
//import org.bobj.common.exception.ErrorResponse;
//import org.bobj.common.response.ApiCommonResponse;
//import org.bobj.payment.dto.VerifyRequestDto;
//import org.bobj.payment.service.PaymentService;
//import org.bobj.point.domain.PointChargeRequestVO;
//import org.bobj.point.service.PointChargeRequestService;
//import org.bobj.point.util.MerchantUidGenerator;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.math.BigDecimal;
//import java.security.Principal;
//
//@RestController
//@RequestMapping("/api/point")
//@RequiredArgsConstructor
//@Log4j2
//@Api(tags = "포인트 충전 및 검증 API")
//public class PointChargeRequestController {
//
//    private final PointChargeRequestService pointChargeRequestService;
//    private final PaymentService paymentService;
//
//    @PostMapping("/charge")
//    @ApiOperation(value = "포인트 충전 요청", notes = "사용자가 지정한 금액으로 포인트 충전 요청을 생성합니다.")
//    @ApiResponses(value = {
//        @ApiResponse(code = 200, message = "충전 요청 성공", response = ApiCommonResponse.class),
//        @ApiResponse(code = 400, message = "잘못된 요청 (금액 누락 등)", response = ErrorResponse.class),
//        @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
//    })
//    public ResponseEntity<ApiCommonResponse<String>> createCharge(
//        @RequestParam @ApiParam(value = "충전 금액", example = "5000", required = true) BigDecimal amount,
//        Principal principal
//    ) {
//        Long userId = Long.parseLong(principal.getName());
//        String merchantUid = MerchantUidGenerator.generate(userId);
//
//        PointChargeRequestVO request = PointChargeRequestVO.builder()
//            .userId(userId)
//            .amount(amount)
//            .merchantUid(merchantUid)
//            .status("PENDING")
//            .build();
//
//        pointChargeRequestService.createChargeRequest(request);
//        log.info("포인트 충전 요청 생성: userId={}, amount={}, merchantUid={}", userId, amount, merchantUid);
//
//        return ResponseEntity.ok(ApiCommonResponse.createSuccess(merchantUid));
//    }
//
//    @PostMapping("/verify")
//    @ApiOperation(value = "결제 검증 및 포인트 지급", notes = "imp_uid를 통해 결제를 검증하고, 결제가 성공한 경우 포인트를 지급합니다.")
//    @ApiResponses(value = {
//        @ApiResponse(code = 200, message = "포인트 충전 성공", response = ApiCommonResponse.class),
//        @ApiResponse(code = 400, message = "잘못된 요청 (금액 불일치, 결제 실패, 중복 처리 등)\n\n" +
//            "**예시:**\n" +
//            "```json\n" +
//            "{\n" +
//            "  \"status\": 400,\n" +
//            "  \"code\": \"P001\",\n" +
//            "  \"message\": \"결제 금액이 일치하지 않습니다.\",\n" +
//            "  \"path\": \"/api/point/verify\"\n" +
//            "}\n" +
//            "```",
//            response = ErrorResponse.class),
//        @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
//    })
//    public ResponseEntity<ApiCommonResponse<String>> verifyPayment(
//        @RequestBody @ApiParam(value = "결제 검증 요청 DTO", required = true)
//        VerifyRequestDto requestDto,
//        Principal principal
//    ) {
//        Long userId = Long.parseLong(principal.getName());
//        log.info("결제 검증 요청: userId={}, impUid={}", userId, requestDto.getImpUid());
//
//        paymentService.verifyPayment(userId, requestDto);
//
//        return ResponseEntity.ok(ApiCommonResponse.createSuccess("포인트 충전 성공"));
//    }
//}
