package org.bobj.property.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.property.dto.PropertyCreateDTO;
import org.bobj.property.dto.PropertyDetailDTO;
import org.bobj.property.dto.PropertyTotalDTO;
import org.bobj.property.service.PropertyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/property")
@RequiredArgsConstructor
@Log4j2
@Api(tags="매물 관리")
public class PropertyController {
    private final PropertyService propertyService;

    @PostMapping
    @ApiOperation(value = "매물 등록", notes = "새로운 매물을 등록합니다.")
    public ResponseEntity<String> createProperty(
            @RequestBody @ApiParam(value = "등록할 매물 정보", required = true) PropertyCreateDTO requestDTO) {
        propertyService.registerProperty(requestDTO);
        return ResponseEntity.ok("매물 등록 완료");
    }

    @GetMapping("/status/{status}")
    @ApiOperation(value = "status 상태의 매물 목록 조회", notes = "[관리자용] 요약 정보가 담긴 매물 목록을 반환합니다.")
    public ResponseEntity<List<PropertyTotalDTO>> getPropertiesByStatus(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @PathVariable("status") String status) {
        List<PropertyTotalDTO> list = propertyService.getAllPropertiesByStatus(status, page, size);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/user/{userId}")
    @ApiOperation(value = "사용자 매물 목록 조회", notes = "[마이페이지용] 특정 사용자가 등록한 매물 목록을 조회합니다.")
    public ResponseEntity<List<PropertyTotalDTO>> getUserProperties(
            @PathVariable @ApiParam(value = "사용자 ID", required = true, example = "1") Long userId) {
        List<PropertyTotalDTO> list = propertyService.getAllPropertiesByUserId(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "매물 상세 조회", notes = "특정 매물의 상세 정보를 반환합니다.")
    public ResponseEntity<PropertyDetailDTO> getPropertyById(
            @PathVariable @ApiParam(value = "매물 ID", required = true, example = "1") Long id) {
        PropertyDetailDTO result = propertyService.getPropertyById(id);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "매물 삭제", notes = "특정 ID의 매물을 삭제합니다.")
    public ResponseEntity<String> deleteProperty(
            @PathVariable @ApiParam(value = "매물 ID", required = true, example = "1") Long id) {
        propertyService.deleteProperty(id);
        return ResponseEntity.ok("매물 삭제 완료");
    }
}
