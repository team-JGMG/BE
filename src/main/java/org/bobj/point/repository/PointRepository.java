package org.bobj.point.repository;


import java.math.BigDecimal;
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

    public BigDecimal findTotalPointByUserId(Long userId) {
        return pointMapper.findTotalPointByUserId(userId);
    }

    // ✅ 추가: 여러 유저의 포인트를 FOR UPDATE로 조회
    public List<PointVO> findByUserIdsForUpdate(List<Long> userIds) {
        return pointMapper.findByUserIdsForUpdate(userIds);
    }

    // ✅ 추가: 여러 포인트를 일괄 업데이트
    public void bulkUpdate(List<PointVO> points) {
        pointMapper.bulkUpdate(points);
    }
}
