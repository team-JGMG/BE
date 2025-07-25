package org.bobj.property.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bobj.property.domain.PropertyVO;

import java.util.List;

@Mapper
public interface PropertyMapper {

    PropertyVO findByPropertyId(@Param("propertyId") Long propertyId);

    List<PropertyVO> findByUserId(@Param("userId") Long userId);

    List<PropertyVO> findTotal(
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit
    );
    void update(@Param("propertyId") Long propertyId, @Param("status") String status);

    void insert(PropertyVO propertyVO);

    void delete(@Param("propertyId") Long propertyId);
}
