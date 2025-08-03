package org.bobj.point.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.bobj.point.domain.PointTransactionVO;

@Mapper
public interface PointTransactionMapper {
    void insert(PointTransactionVO transaction);
    List<PointTransactionVO> findByUserId(Long userId);
}
