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

    public BigDecimal getTotalPoint(Long userId) {
        // 포인트 테이블에서 현재 보유 포인트 계산
        return pointRepository.findTotalPointByUserId(userId);
    }

    @Transactional
    public void requestRefund(Long userId, BigDecimal amount) {
        // 1. 유저 포인트 조회 (락 걸기 위해 for update)
        PointVO point = pointRepository.findByUserIdForUpdate(userId);
        if (point == null) {
            throw new IllegalStateException("포인트 정보가 존재하지 않습니다.");
        }

        // 2. 잔액 확인
        if (point.getAmount().compareTo(amount) < 0) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }

        // 3. 포인트 차감
        point.setAmount(point.getAmount().subtract(amount));
        pointRepository.update(point);

        // 4. 트랜잭션 기록 (WITHDRAW)
        PointTransactionVO tx = PointTransactionVO.builder()
            .pointId(point.getPointId())
            .type(PointTransactionType.WITHDRAW)
            .amount(amount)
            .createdAt(LocalDateTime.now())
            .build();

        pointTransactionRepository.insert(tx);
    }

    //userId에게 amount만큼 환급 포인트를 지급
    @Transactional
    public void refundForShareSell(Long userId, BigDecimal amount) {
        PointVO point = pointRepository.findByUserIdForUpdate(userId);
        if (point == null) {
            point = PointVO.builder().userId(userId).amount(amount).build();
            pointRepository.insert(point);
        } else {
            point.setAmount(point.getAmount().add(amount));
            pointRepository.update(point);
        }

        //트랜잭션 로그로 남기기 위해 PointTransactionVO 객체를 생성
        PointTransactionVO tx = PointTransactionVO.builder()
            .pointId(point.getPointId())
            .type(PointTransactionType.REFUND)
            .amount(amount)
            .createdAt(LocalDateTime.now())
            .build();
        //위에서 만든 트랜잭션 기록을 POINT_TRANSACTION 테이블에 insert
        pointTransactionRepository.insert(tx);
    }



    @Transactional
    public void investPoint(Long userId, BigDecimal amount) {
        PointVO point = pointRepository.findByUserIdForUpdate(userId);
        if (point == null || point.getAmount().compareTo(amount) < 0) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }

        // 포인트 차감
        point.setAmount(point.getAmount().subtract(amount));
        pointRepository.update(point);

        // 트랜잭션 기록
        PointTransactionVO tx = PointTransactionVO.builder()
            .pointId(point.getPointId())
            .type(PointTransactionType.INVEST) // enum에 정의돼 있어야 함
            .amount(amount)
            .createdAt(LocalDateTime.now())
            .build();

        pointTransactionRepository.insert(tx);
    }

    /**
     * 펀딩 실패 시 포인트 환급
     */
    @Transactional
    public void refundForFundingFailure(Long userId, BigDecimal amount) {
        PointVO point = pointRepository.findByUserIdForUpdate(userId);

        if (point == null) {
            point = PointVO.builder()
                .userId(userId)
                .amount(amount)
                .build();
            pointRepository.insert(point);
        } else {
            point.setAmount(point.getAmount().add(amount));
            pointRepository.update(point);
        }

        PointTransactionVO tx = PointTransactionVO.builder()
            .pointId(point.getPointId())
            .type(PointTransactionType.REFUND)
            .amount(amount)
            .createdAt(LocalDateTime.now())
            .build();

        pointTransactionRepository.insert(tx);
    }



}