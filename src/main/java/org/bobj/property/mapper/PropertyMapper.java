package org.bobj.property.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bobj.property.domain.PropertyVO;
import org.bobj.property.dto.PropertySoldResponseDTO;
import org.bobj.property.dto.PropertyUserResponseDTO;

import java.util.List;

@Mapper
public interface PropertyMapper {

    PropertyVO findByPropertyId(@Param("propertyId") Long propertyId);

    List<String> findHashtagNamesByPropertyId(Long propertyId);

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

    // 사진 업로드
    void insertPropertyPhoto(@Param("propertyId") Long propertyId,
                             @Param("photoUrl") String photoUrl);

    // 문서 업로드
    void insertPropertyDocument(@Param("propertyId") Long propertyId,
                                @Param("documentType") String documentType,
                                @Param("fileUrl") String fileUrl);

    void updatePropertiesAsSold(@Param("propertyIds") List<Long> propertyIds);

    void updateRentalIncome(@Param("propertyId") Long propertyId, @Param("rentalIncome") java.math.BigDecimal rentalIncome);
    
    /**
     * 매물 ID로 법정동 코드 조회
     */
    String findRawdCdByPropertyId(@Param("propertyId") Long propertyId);
}
