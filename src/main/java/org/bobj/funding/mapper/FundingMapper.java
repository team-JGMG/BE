package org.bobj.funding.mapper;


import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bobj.funding.domain.FundingOrderVO;
import org.bobj.funding.domain.FundingVO;
import org.bobj.funding.dto.FundingDetailResponseDTO;
import org.bobj.funding.dto.FundingEndedResponseDTO;
import org.bobj.funding.dto.FundingTotalResponseDTO;

@Mapper
public interface FundingMapper {
    FundingDetailResponseDTO findFundingById(@Param("fundingId") Long fundingId);

    List<FundingTotalResponseDTO> findTotal(
            @Param("category") String category,
            @Param("sort") String sort,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    List<FundingEndedResponseDTO> findEndedFundingProperties(@Param("offset") int offset, @Param("limit") int limit);

    void insertFunding(@Param("propertyId") Long propertyId);

    void increaseCurrentAmount(@Param("fundingId") Long fundingId, @Param("orderPrice") BigDecimal orderPrice);

    void decreaseCurrentAmount(@Param("fundingId") Long fundingId, @Param("orderPrice") BigDecimal orderPrice);

    void expireFunding(@Param("fundingId") Long fundingId);
}
