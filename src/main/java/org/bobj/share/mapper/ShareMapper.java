package org.bobj.share.mapper;

import org.apache.ibatis.annotations.Param;

public interface ShareMapper {
    Integer findUserShareCount(@Param("userId") Long userId,
                           @Param("fundingId") Long fundingId);
}
