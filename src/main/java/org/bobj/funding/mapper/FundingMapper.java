package org.bobj.funding.mapper;


import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bobj.funding.domain.FundingOrderVO;
import org.bobj.funding.domain.FundingVO;

@Mapper
public interface FundingMapper {
    FundingVO findFundingById(@Param("fundingId") Long fundingId);

    void insertFundingOrder(FundingOrderVO fundingOrder);

    void cancelFundingOrder(@Param("orderId") Long orderId);

    List<FundingOrderVO> findOrdersByUserIdAndFundingId(@Param("userId") Long userId, @Param("fundingId") Long fundingId);
}
