package org.bobj.point.repository;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bobj.point.domain.PointTransactionVO;
import org.bobj.point.mapper.PointTransactionMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointTransactionRepository {
    private final PointTransactionMapper pointTransactionMapper;

    public void insert(PointTransactionVO transaction){
        pointTransactionMapper.insert(transaction);
    }


    public List<PointTransactionVO> findByUserId(Long userId){
        return pointTransactionMapper.findByUserId(userId);
    }


}
