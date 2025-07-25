package org.bobj.property.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.property.domain.PropertyVO;
import org.bobj.property.dto.PropertyCreateDTO;
import org.bobj.property.dto.PropertyDetailDTO;
import org.bobj.property.dto.PropertyTotalDTO;
import org.bobj.property.mapper.PropertyMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyMapper propertyMapper;

    /**
     * 매물 등록
     */
    public void registerProperty(PropertyCreateDTO dto) {
        PropertyVO vo = dto.toVO();
        propertyMapper.insert(vo);
    }

    /**

     * 매물 전체 조회 -> 관리자용
     */
    public List<PropertyTotalDTO> getAllPropertiesByStatus(String status,int page, int size) {
        int offset = page * size;
        List<PropertyVO> vos = propertyMapper.findTotal(status, offset, size);
        return vos.stream()
                .map(PropertyTotalDTO::of)
                .collect(Collectors.toList());
    }

    // User 매물 조회
    public List<PropertyTotalDTO> getAllPropertiesByUserId(Long userId) {
        List<PropertyVO> vos = propertyMapper.findByUserId(userId);
        return vos.stream()
                .map(PropertyTotalDTO::of)
                .collect(Collectors.toList());
    }

    /**

     * 매물 단건 조회 -> 관리자용, 마이페이지용
     */
    public PropertyDetailDTO getPropertyById(Long propertyId) {
        PropertyVO vo = propertyMapper.findByPropertyId(propertyId);
        return PropertyDetailDTO.of(vo);
    }

    /**
     * 매물 삭제
     */
    public void deleteProperty(Long propertyId) {
        propertyMapper.delete(propertyId);
    }
}
