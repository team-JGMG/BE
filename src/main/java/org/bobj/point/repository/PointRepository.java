package org.bobj.point.repository;


import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bobj.point.domain.PointVO;
import org.bobj.point.mapper.PointMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointRepository {

    private final PointMapper pointMapper;

    public PointVO findById(Long pointId){
        return pointMapper.findById(pointId);
    }

    public PointVO findByUserId(Long userId){
        return pointMapper.findByUserId(userId);
    }

    public void insert(PointVO point){
        pointMapper.insert(point);
    };

    public void update(PointVO point){
        pointMapper.update(point);
    };

    public void delete(Long pointId){
        pointMapper.delete(pointId);
    };

    public PointVO findByUserIdForUpdate(Long userId){
        return pointMapper.findByUserIdForUpdate(userId);
    }
}
