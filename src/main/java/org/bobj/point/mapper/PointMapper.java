package org.bobj.point.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bobj.point.domain.PointVO;

@Mapper
public interface PointMapper {

    PointVO findById(@Param("pointId") Long pointId);

    PointVO findByUserId(@Param("userId") Long userId);

    void insert(PointVO point);

    void update(PointVO point);

    void delete(@Param("pointId") Long pointId);

    PointVO findByUserIdForUpdate(@Param("userId") Long userId);


    BigDecimal findTotalPointByUserId(Long userId);

    // ✅ 추가: 여러 유저의 포인트를 FOR UPDATE로 조회
    List<PointVO> findByUserIdsForUpdate(@Param("userIds") List<Long> userIds);

    // ✅ 추가: 여러 포인트를 한 번에 업데이트
    void bulkUpdate(@Param("points") List<PointVO> points);
}
