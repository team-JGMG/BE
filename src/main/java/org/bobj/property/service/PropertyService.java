package org.bobj.property.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.bobj.funding.mapper.FundingMapper;
import org.bobj.property.domain.PropertyVO;
import org.bobj.property.dto.PropertyCreateDTO;
import org.bobj.property.dto.PropertyDetailDTO;
import org.bobj.property.dto.PropertyTotalDTO;
import org.bobj.property.mapper.PropertyMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public List<PropertyTotalDTO> getAllPropertiesByStatus(String status,int page, int size) {
        int offset = page * size;
        List<PropertyVO> vos = propertyMapper.findTotal(status, offset, size);
        return vos.stream()
                .map(PropertyTotalDTO::of)
                .collect(Collectors.toList());
    }

    // 유저의 매물 리스트 조회(마이페이지)
    public List<PropertyTotalDTO> getAllPropertiesByUserId(Long userId) {
        List<PropertyVO> vos = propertyMapper.findByUserId(userId);
        return vos.stream()
                .map(PropertyTotalDTO::of)
                .collect(Collectors.toList());
    }

    // 매물 상세 조회(관리자, 마이페이지(내가 올린 매물)
    public PropertyDetailDTO getPropertyById(Long propertyId) {
        PropertyVO vo = propertyMapper.findByPropertyId(propertyId);
        return PropertyDetailDTO.of(vo);
    }
}
