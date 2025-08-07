package org.bobj.point.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.bobj.point.domain.PointTransactionVO;

@Mapper
public interface PointTransactionMapper {
    void insert(PointTransactionVO transaction);
    List<PointTransactionVO> findByUserId(Long userId);
    // ✅ 추가: 거래내역 bulk insert
    void bulkInsert(List<PointTransactionVO> transactions);
}
