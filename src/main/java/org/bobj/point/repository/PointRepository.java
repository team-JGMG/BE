package org.bobj.point.repository;

import com.sun.org.apache.bcel.internal.generic.ARETURN;
import com.sun.org.apache.bcel.internal.generic.DRETURN;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Param;
import org.bobj.point.domain.PointVO;
import org.bobj.point.mapper.PointMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointRepository {

    private final PointMapper pointMapper;

    PointVO findById(Long pointId){
        return pointMapper.findById(pointId);
    }

    List<PointVO> findByUserId(Long userId){
        return pointMapper.findByUserId(userId);
    }

    void insert(PointVO point){
        pointMapper.insert(point);
    };

    void update(PointVO point){
        pointMapper.update(point);
    };

    void delete(Long pointId){
        pointMapper.delete(pointId);
    };

}
