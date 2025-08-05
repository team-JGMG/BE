package org.bobj.share.mapper;

import org.apache.ibatis.annotations.Param;
import org.bobj.share.domain.ShareVO;
import org.bobj.share.dto.response.ShareResponseDTO;

import java.math.BigDecimal;
import java.util.List;

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

    List<ShareResponseDTO> findSharesByUserId(@Param("userId") Long userId);

    List<ShareResponseDTO> findSharesByUserIdPaging(@Param("userId") Long userId,
                                                    @Param("offset") int offset,
                                                    @Param("limit") int limit);

    int countSharesByUserId(@Param("userId") Long userId);

    //주식 데이터 배치 삽입
    int insertSharesBatch(List<ShareVO> shares);

    List<ShareVO> findByFundingId(Long fundingId);

}
