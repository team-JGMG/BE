package org.bobj.property.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bobj.property.domain.PropertyStatus;
import org.bobj.property.domain.PropertyVO;
import org.bobj.property.dto.PropertySoldResponseDTO;
import org.bobj.property.dto.PropertyUserResponseDTO;

import java.util.List;

@Mapper
public interface PropertyMapper {

    PropertyVO findByPropertyId(@Param("propertyId") Long propertyId);

    List<PropertyUserResponseDTO> findByUserId(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    List<PropertySoldResponseDTO> findSold();

    List<PropertyVO> findTotal(
            @Param("category") String category,
            @Param("offset") int offset,
            @Param("limit") int limit
    );
    void update(@Param("propertyId") Long propertyId, @Param("status") String status);

    void insert(PropertyVO propertyVO);
}
