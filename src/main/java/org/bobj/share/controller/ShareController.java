package org.bobj.share.controller;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.common.exception.ErrorResponse;
import org.bobj.common.response.ApiCommonResponse;
import org.bobj.share.dto.response.ShareResponseDTO;
import org.bobj.share.service.ShareService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shares")
@RequiredArgsConstructor
@Log4j2
@Api(tags="지분 API")
public class ShareController {
    private final ShareService shareService;

    @GetMapping("/users/{userId}") // 엔드포인트 정의
    @ApiOperation(value = "사용자 보유 지분 조회", notes = "특정 사용자가 보유한 모든 주식 지분 정보를 조회합니다.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "조회할 사용자 ID", required = true, dataType = "long", paramType = "path"),
            @ApiImplicitParam(name = "page", value = "페이지 번호 (0부터 시작)", defaultValue = "0", dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "size", value = "한 페이지당 항목 수", defaultValue = "10", dataType = "int", paramType = "query")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "사용자 보유 지분 조회 성공", response = ShareResponseDTO.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "잘못된 요청 (예: 유효하지 않은 사용자 ID)", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "서버 내부 오류", response = ErrorResponse.class)
    })
    public ResponseEntity<ApiCommonResponse<List<ShareResponseDTO>>> getUserShares(
            @PathVariable @ApiParam(value = "사용자 ID", required = true) Long userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        List<ShareResponseDTO> userShares = shareService.getSharesByUserIdPaging(userId, page, size);

        return ResponseEntity.ok(ApiCommonResponse.createSuccess(userShares));
    }
}
