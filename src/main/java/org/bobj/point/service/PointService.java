package org.bobj.point.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bobj.point.domain.PointTransactionType;
import org.bobj.point.domain.PointTransactionVO;
import org.bobj.point.domain.PointVO;
import org.bobj.point.repository.PointRepository;
import org.bobj.point.repository.PointTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;
    private final PointTransactionRepository pointTransactionRepository;

    public PointVO findById(Long pointId) {
        return pointRepository.findById(pointId);
    }

    public PointVO findByUserId(Long userId) {
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

    public PointVO findByUserIdForUpdate(Long userId){
        return pointRepository.findByUserIdForUpdate(userId);
    }


    public List<PointTransactionVO> findTransactionsByUserId(Long userId) {
        return pointTransactionRepository.findByUserId(userId);
    }



    /**
     *
     * 결제 성공 후 포인트 적립 및 트랜잭션 기록
     * @param userId 사용자 ID
     * @param amount 충전 금액
     * @param impUid 결제 고유 ID (PortOne)
     */
    @Transactional
    public void chargePoint(Long userId, BigDecimal amount, String impUid) {
        // 1. 유저의 포인트 정보 조회 (FOR UPDATE)
        PointVO point = pointRepository.findByUserIdForUpdate(userId);

        // 2. 없으면 새로 생성
        if (point == null) {
            point = PointVO.builder()
                .userId(userId)
                .amount(amount)
                .build();
            pointRepository.insert(point);
        } else {
            // 3. 있으면 잔액 누적
            point.setAmount(point.getAmount().add(amount));
            pointRepository.update(point);
        }

        // 4. 트랜잭션 기록 (DEPOSIT)
        PointTransactionVO tx = PointTransactionVO.builder()
            .pointId(point.getPointId())
            .type(PointTransactionType.DEPOSIT)
            .amount(amount)
            .createdAt(LocalDateTime.now())
            .build();

        pointTransactionRepository.insert(tx);
    }
}