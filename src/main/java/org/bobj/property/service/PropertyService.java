package org.bobj.property.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.common.dto.CustomSlice;
import org.bobj.funding.mapper.FundingMapper;
import org.bobj.property.domain.PropertyStatus;
import org.bobj.property.domain.PropertyVO;
import org.bobj.property.dto.*;
import org.bobj.property.mapper.PropertyMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyService {
    private final PropertyMapper propertyMapper;
    private final FundingMapper fundingMapper;

    // 매물 승인 + 펀딩 등록 or 거절
    @Transactional
    public void updatePropertyStatus(Long propertyId, String status) {
        propertyMapper.update(propertyId,status);

        // 상태가 APPROVED일 때만 fundings 테이블에 insert
        if ("APPROVED".equalsIgnoreCase(status)) {
            fundingMapper.insertFunding(propertyId);
        }
    }

    // 매물 등록
    public void registerProperty(PropertyCreateDTO dto) {
        PropertyVO vo = dto.toVO();
        propertyMapper.insert(vo);
    }

    // 매물 리스트 조회(관리자 페이지)
    public CustomSlice<PropertyTotalDTO> getAllPropertiesByStatus(String category, int page, int size) {
        int offset = page * size;

        List<PropertyVO> vos = propertyMapper.findTotal(category, offset, size+1);
        boolean hasNext = vos.size() > size;
        if(hasNext) {
            vos.remove(size);
        }

        List<PropertyTotalDTO> dtoList = vos.stream()
                .map(PropertyTotalDTO::of)
                .collect(Collectors.toList());
        return new CustomSlice<>(dtoList,hasNext);
    }

    // 유저의 매물 리스트 조회(마이페이지)
    public CustomSlice<PropertyUserResponseDTO> getUserPropertiesByStatus(Long userId, String status, int page, int size) {
        int offset = page * size;
        String upperStatus = status.toUpperCase();

        List<PropertyUserResponseDTO> resultList =
                propertyMapper.findByUserId(userId, upperStatus, offset, size + 1);

        boolean hasNext = resultList.size() > size;
        if (hasNext) {
            resultList.remove(size);
        }

        // 펀딩 관련 계산 (APPROVED, SOLD일 때만 적용)
        if (upperStatus.equals("APPROVED") || upperStatus.equals("SOLD")) {
            for (PropertyUserResponseDTO dto : resultList) {
                if (dto.getTargetAmount() != null
                        && dto.getCurrentAmount() != null
                        && dto.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {

                    BigDecimal target = dto.getTargetAmount();
                    BigDecimal current = dto.getCurrentAmount();

                    int rate = current
                            .multiply(BigDecimal.valueOf(100))
                            .divide(target, 0, RoundingMode.DOWN)
                            .intValue();
                    dto.setAchievementRate(rate);

                    BigDecimal remainingAmount = target.subtract(current);
                    dto.setRemainingAmount(remainingAmount);

                    long remainingShares = remainingAmount
                            .divide(BigDecimal.valueOf(5000), 0, RoundingMode.DOWN)
                            .longValue();
                    dto.setRemainingShares(remainingShares);
                }
            }
        }

        return new CustomSlice<>(resultList, hasNext);
    }

    // 매물 상세 조회(관리자, 마이페이지(내가 올린 매물)
    public PropertyDetailDTO getPropertyById(Long propertyId) {
        PropertyVO vo = propertyMapper.findByPropertyId(propertyId);
        return PropertyDetailDTO.of(vo);
    }

    // 매각 완료 매물 리스트
    public List<PropertySoldResponseDTO> getSoldProperties() {
        return propertyMapper.findSold();
    }
}
