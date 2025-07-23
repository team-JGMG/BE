package org.bobj.point.service;


import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bobj.point.domain.PointVO;
import org.bobj.point.repository.PointRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {
    private final PointRepository pointRepository;

    public PointVO findById(Long pointId) {
        return pointRepository.findById(pointId);
    }

    public List<PointVO> findByUserId(Long userId) {
        return pointRepository.findByUserId(userId);
    }

    public void createPoint(PointVO point) {
        pointRepository.insert(point);
    }

    public void updatePoint(PointVO point) {
        pointRepository.update(point);
    }

    public void deletePoint(Long pointId) {
        pointRepository.delete(pointId);
    }



}
