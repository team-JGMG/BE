package org.bobj.share.mapper;

import org.apache.ibatis.annotations.Param;
import org.bobj.share.domain.ShareVO;

import java.math.BigDecimal;

public interface ShareMapper {
    Integer findUserShareCount(@Param("userId") Long userId,
                               @Param("fundingId") Long fundingId);

    void insert(ShareVO shareVO);

    void update(@Param("shareId") Long shareId,
                @Param("shareCount") int shareCount,
                @Param("averageAmount") BigDecimal averageAmount);

    void delete(@Param("shareId") Long shareId); // 또는 deleteShareByUserIdAndFundingId

    ShareVO findUserShareByFundingIdForUpdate(@Param("userId") Long userId,
                                              @Param("fundingId") Long fundingId);
}
