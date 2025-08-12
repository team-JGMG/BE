package org.bobj.common.crypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.common.response.ApiCommonResponse;
import org.bobj.property.dto.PropertyDetailDTO;
import org.bobj.property.dto.SellerDTO;
import org.bobj.user.dto.response.UserResponseDTO;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 🔓 API 응답 자동 복호화 처리기
 * 
 * 모든 REST API 응답에서 암호화된 개인정보를 자동으로 복호화합니다.*
 * 지원하는 복호화 대상:
 * - UserResponseDTO: 이름, 전화번호, 계좌번호, 은행코드
 * - SellerDTO: 판매자 이름, 전화번호  
 * - PropertyDetailDTO: 포함된 판매자 정보 복호화
 * - List 형태의 위 객체들
 * - ApiCommonResponse로 래핑된 위 객체들
 * 
 * 특징:
 * - 기존 Controller/Service 코드 변경 불필요
 * - 복호화 실패 시 원본 데이터 반환 (안전성)
 * - 성능 최적화: HTTP 응답 시점에만 복호화
 * - 로깅: 복호화 성공/실패 상황 추적
 * 
 * @author BOBJ Team
 * @since 2025-01-01
 */
@RestControllerAdvice
@Component
@RequiredArgsConstructor
@Slf4j
public class DecryptionResponseAdvice implements ResponseBodyAdvice<Object> {
    
    private final PersonalDataCrypto personalDataCrypto;

    /**
     * 처리 대상 응답인지 판단
     * org.bobj 패키지의 모든 Controller 응답을 처리합니다.
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // BOBJ 프로젝트의 모든 Controller 응답 처리
        String packageName = returnType.getContainingClass().getPackage().getName();
        boolean shouldProcess = packageName.startsWith("org.bobj");
        
        if (shouldProcess) {
            log.debug("✅ 복호화 처리 대상: {}.{}", 
                returnType.getContainingClass().getSimpleName(), 
                returnType.getMethod().getName());
        }
        
        return shouldProcess;
    }

    /**
     * HTTP 응답 직전에 객체를 자동 복호화합니다.
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, 
                                MediaType selectedContentType, 
                                Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                ServerHttpRequest request, 
                                ServerHttpResponse response) {
        
        if (body == null) {
            return null;
        }

        try {
            Object decryptedBody = decryptResponseObject(body);
            
            // 복호화가 실제로 수행되었는지 로깅
            if (decryptedBody != body) {
                log.info("🔓 응답 객체 복호화 완료: {} -> {}", 
                    body.getClass().getSimpleName(), 
                    request.getURI().getPath());
            }
            
            return decryptedBody;
            
        } catch (Exception e) {
            log.error("❌ 응답 복호화 중 오류 발생 - 원본 반환: {} at {}", 
                e.getMessage(), request.getURI().getPath(), e);
            return body; // 오류 시 원본 반환으로 안전성 확보
        }
    }

    /**
     * 응답 객체 타입에 따라 적절한 복호화 수행
     */
    private Object decryptResponseObject(Object obj) {
        if (obj == null) {
            return null;
        }
        
        // 1. ApiCommonResponse로 래핑된 경우
        if (obj instanceof ApiCommonResponse) {
            return decryptApiCommonResponse((ApiCommonResponse<?>) obj);
        }
        
        // 2. List 형태인 경우
        if (obj instanceof List) {
            return decryptList((List<?>) obj);
        }
        
        // 3. 개별 DTO 복호화
        if (obj instanceof UserResponseDTO) {
            return decryptUserResponseDTO((UserResponseDTO) obj);
        }
        
        if (obj instanceof SellerDTO) {
            return decryptSellerDTO((SellerDTO) obj);
        }
        
        if (obj instanceof PropertyDetailDTO) {
            return decryptPropertyDetailDTO((PropertyDetailDTO) obj);
        }
        
        if (obj instanceof org.bobj.funding.dto.FundingDetailResponseDTO) {
            return decryptFundingDetailResponseDTO((org.bobj.funding.dto.FundingDetailResponseDTO) obj);
        }
        
        // PropertyTotalDTO는 seller 정보가 없으므로 복호화 불필요
        // ShareResponseDTO, OrderBookResponseDTO 등은 개인정보가 없으므로 복호화 불필요
        
        // 복호화 대상이 아닌 경우 원본 반환
        return obj;
    }

    /**
     * ApiCommonResponse 래핑 객체 복호화
     */
    private ApiCommonResponse<?> decryptApiCommonResponse(ApiCommonResponse<?> response) {
        if (response.getData() == null) {
            return response;
        }
        
        Object decryptedData = decryptResponseObject(response.getData());
        
        // 복호화된 데이터로 새로운 응답 생성
        return ApiCommonResponse.createSuccess(decryptedData);
    }

    /**
     * List 형태 객체들 복호화
     */
    private List<?> decryptList(List<?> list) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        
        return list.stream()
            .map(this::decryptResponseObject)
            .collect(Collectors.toList());
    }

    /**
     * 사용자 정보 DTO 복호화
     */
    private UserResponseDTO decryptUserResponseDTO(UserResponseDTO dto) {
        if (dto == null) {
            return null;
        }
        
        try {
            // 복사본 생성 (원본 수정 방지)
            UserResponseDTO decrypted = UserResponseDTO.builder()
                .userId(dto.getUserId())
                .email(dto.getEmail())        // 이메일은 암호화 안됨
                .nickname(dto.getNickname())  // 닉네임은 암호화 안됨  
                .isAdmin(dto.getIsAdmin())
                .role(dto.getRole())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();

            // 암호화된 필드들을 복호화
            if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
                decrypted.setName(safeDecrypt(dto.getName(), PersonalDataCrypto.FieldType.NAME));
                log.debug("✅ 사용자 이름 복호화 완료: userId={}", dto.getUserId());
            }
            
            if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
                decrypted.setPhone(safeDecrypt(dto.getPhone(), PersonalDataCrypto.FieldType.PHONE));
                log.debug("✅ 사용자 전화번호 복호화 완료: userId={}", dto.getUserId());
            }
            
            if (dto.getAccountNumber() != null && !dto.getAccountNumber().trim().isEmpty()) {
                decrypted.setAccountNumber(safeDecrypt(dto.getAccountNumber(), PersonalDataCrypto.FieldType.ACCOUNT_NUMBER));
                log.debug("✅ 사용자 계좌번호 복호화 완료: userId={}", dto.getUserId());
            }
            
            if (dto.getBankCode() != null && !dto.getBankCode().trim().isEmpty()) {
                decrypted.setBankCode(safeDecrypt(dto.getBankCode(), PersonalDataCrypto.FieldType.BANK_CODE));
                log.debug("✅ 사용자 은행코드 복호화 완료: userId={}", dto.getUserId());
            }

            log.info("🔓 UserResponseDTO 복호화 완료 - userId: {}", dto.getUserId());
            return decrypted;
            
        } catch (Exception e) {
            log.warn("⚠️ UserResponseDTO 복호화 실패 - 원본 반환: userId={}, error={}", 
                dto.getUserId(), e.getMessage());
            return dto; // 복호화 실패시 원본 반환
        }
    }

    /**
     * 판매자 정보 DTO 복호화
     */
    private SellerDTO decryptSellerDTO(SellerDTO dto) {
        if (dto == null) {
            return null;
        }
        
        try {
            SellerDTO decrypted = SellerDTO.builder()
                .userId(dto.getUserId())
                .email(dto.getEmail())  // 이메일은 암호화 안됨
                .build();

            // 암호화된 필드들 복호화
            if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
                decrypted.setName(safeDecrypt(dto.getName(), PersonalDataCrypto.FieldType.NAME));
                log.debug("✅ 판매자 이름 복호화 완료: userId={}", dto.getUserId());
            }
            
            if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
                decrypted.setPhone(safeDecrypt(dto.getPhone(), PersonalDataCrypto.FieldType.PHONE));
                log.debug("✅ 판매자 전화번호 복호화 완료: userId={}", dto.getUserId());
            }

            log.info("🔓 SellerDTO 복호화 완료 - userId: {}", dto.getUserId());
            return decrypted;
            
        } catch (Exception e) {
            log.warn("⚠️ SellerDTO 복호화 실패 - 원본 반환: userId={}, error={}", 
                dto.getUserId(), e.getMessage());
            return dto;
        }
    }

    /**
     * 펀딩 상세 DTO 복호화 (포함된 SellerDTO만 복호화)
     * Legacy 환경에서 Controller가 직접 호출할 수 있도록 public으로 제공
     */
    public org.bobj.funding.dto.FundingDetailResponseDTO decryptFundingDetailResponseDTO(org.bobj.funding.dto.FundingDetailResponseDTO dto) {
        if (dto == null) {
            return null;
        }
        
        try {
            // 판매자 정보가 있으면 복호화
            SellerDTO originalSeller = dto.getSeller();
            SellerDTO decryptedSeller = decryptSellerDTO(originalSeller);
            
            // 복호화가 필요한 경우에만 새 객체 생성
            if (decryptedSeller != originalSeller) {
                org.bobj.funding.dto.FundingDetailResponseDTO decrypted = org.bobj.funding.dto.FundingDetailResponseDTO.builder()
                    .fundingId(dto.getFundingId())
                    .propertyId(dto.getPropertyId())
                    .title(dto.getTitle())
                    .address(dto.getAddress())
                    .targetAmount(dto.getTargetAmount())
                    .fundingRate(dto.getFundingRate())
                    .currentAmount(dto.getCurrentAmount())
                    .fundingEndDate(dto.getFundingEndDate())
                    .daysLeft(dto.getDaysLeft())
                    .currentShareAmount(dto.getCurrentShareAmount())
                    .usageDistrict(dto.getUsageDistrict())
                    .landArea(dto.getLandArea())
                    .buildingArea(dto.getBuildingArea())
                    .totalFloorAreaProperty(dto.getTotalFloorAreaProperty())
                    .totalFloorAreaBuilding(dto.getTotalFloorAreaBuilding())
                    .basementFloors(dto.getBasementFloors())
                    .groundFloors(dto.getGroundFloors())
                    .approvalDate(dto.getApprovalDate())
                    .officialLandPrice(dto.getOfficialLandPrice())
                    .unitPricePerPyeong(dto.getUnitPricePerPyeong())
                    .description(dto.getDescription())
                    .expectedDividendPerShare(dto.getExpectedDividendPerShare())
                    .photos(dto.getPhotos())
                    .seller(decryptedSeller)  // 복호화된 판매자 정보
                    .createdAt(dto.getCreatedAt())
                    .tags(dto.getTags())
                    .build();

                return decrypted;
            }
            
            // 복호화가 필요없는 경우 원본 반환
            return dto;
            
        } catch (Exception e) {
            log.warn("⚠️ FundingDetailResponseDTO 복호화 실패 - 원본 반환: fundingId={}, error={}", 
                dto.getFundingId(), e.getMessage());
            return dto;
        }
    }

    /**
     * 매물 상세 DTO 복호화 (포함된 판매자 정보만 복호화)
     * Legacy 환경에서 Controller가 직접 호출할 수 있도록 public으로 제공
     */
    public PropertyDetailDTO decryptPropertyDetailDTO(PropertyDetailDTO dto) {
        if (dto == null) {
            return null;
        }
        
        try {
            // 판매자 정보가 있으면 복호화
            SellerDTO originalSeller = dto.getSeller();
            SellerDTO decryptedSeller = decryptSellerDTO(originalSeller);
            
            // 복호화가 필요한 경우에만 새 객체 생성
            if (decryptedSeller != originalSeller) {
                PropertyDetailDTO decrypted = PropertyDetailDTO.builder()
                    .propertyId(dto.getPropertyId())
                    .userId(dto.getUserId())
                    .seller(decryptedSeller)  // 복호화된 판매자 정보
                    .title(dto.getTitle())
                    .address(dto.getAddress())
                    .area(dto.getArea())
                    .price(dto.getPrice())
                    .postingPeriod(dto.getPostingPeriod())
                    .status(dto.getStatus())
                    .usageDistrict(dto.getUsageDistrict())
                    .landArea(dto.getLandArea())
                    .buildingArea(dto.getBuildingArea())
                    .totalFloorAreaProperty(dto.getTotalFloorAreaProperty())
                    .totalFloorAreaBuilding(dto.getTotalFloorAreaBuilding())
                    .basementFloors(dto.getBasementFloors())
                    .groundFloors(dto.getGroundFloors())
                    .approvalDate(dto.getApprovalDate())
                    .officialLandPrice(dto.getOfficialLandPrice())
                    .unitPricePerPyeong(dto.getUnitPricePerPyeong())
                    .propertyType(dto.getPropertyType())
                    .roomCount(dto.getRoomCount())
                    .bathroomCount(dto.getBathroomCount())
                    .floor(dto.getFloor())
                    .description(dto.getDescription())
                    .createdAt(dto.getCreatedAt())
                    .updatedAt(dto.getUpdatedAt())
                    .soldAt(dto.getSoldAt())
                    .documents(dto.getDocuments())
                    .photos(dto.getPhotos())
                    .tags(dto.getTags())
                    .build();

                return decrypted;
            }
            
            // 복호화가 필요없는 경우 원본 반환
            return dto;
            
        } catch (Exception e) {
            log.warn("⚠️ PropertyDetailDTO 복호화 실패 - 원본 반환: propertyId={}, error={}", 
                dto.getPropertyId(), e.getMessage());
            return dto;
        }
    }

    /**
     * SellerDTO 수동 복호화 (Legacy 환경용 public 메서드)
     */
    public SellerDTO decryptSellerDTOManual(SellerDTO dto) {
        SellerDTO result = decryptSellerDTO(dto);
        log.debug("🔧 SellerDTO 수동 복호화 호출됨");
        return result;
    }

    /**
     * 안전한 복호화 헬퍼 메서드
     */
    private String safeDecrypt(String encryptedValue, PersonalDataCrypto.FieldType fieldType) {
        if (encryptedValue == null || encryptedValue.trim().isEmpty()) {
            return encryptedValue;
        }
        
        try {
            String decrypted = personalDataCrypto.decrypt(encryptedValue, fieldType);
            log.debug("🔓 개별 필드 복호화 성공: {} 타입", fieldType);
            return decrypted;
        } catch (Exception e) {
            log.warn("⚠️ 개별 필드 복호화 실패 - 원본 반환: fieldType={}, error={}", 
                fieldType, e.getMessage());
            return encryptedValue; // 실패시 원본 반환
        }
    }
}
