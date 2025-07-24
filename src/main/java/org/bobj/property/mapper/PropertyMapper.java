package org.bobj.property.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bobj.property.domain.PropertyVO;

import java.util.List;

@Mapper
public interface PropertyMapper {
    PropertyVO findById(@Param("propertyId") Long propertyId);

    List<PropertyVO> findTotal();

    void insert(PropertyVO propertyVO);

    void delete(@Param("propertyId") Long propertyId);
}
