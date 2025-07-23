package org.bobj.point.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bobj.point.domain.PointVO;

@Mapper
public interface PointMapper {

    PointVO findById(@Param("pointId") Long pointId);

    List<PointVO> findByUserId(@Param("userId") Long userId);

    void insert(PointVO point);

    void update(PointVO point);

    void delete(@Param("pointId") Long pointId);
}
