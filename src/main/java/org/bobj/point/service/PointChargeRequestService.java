package org.bobj.point.service;

import lombok.RequiredArgsConstructor;
import org.bobj.point.domain.PointChargeRequestVO;
import org.bobj.point.repository.PointChargeRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointChargeRequestService {

    private final PointChargeRequestRepository pointChargeRequestRepository;

    /**
     * 포인트 충전 요청 저장 (PENDING 상태로)
     */
    @Transactional
    public void createChargeRequest(PointChargeRequestVO request) {
        pointChargeRequestRepository.save(request);
    }

    /**
     * merchant_uid 로 충전 요청 조회
     */
    public PointChargeRequestVO findByMerchantUid(String merchantUid) {
        return pointChargeRequestRepository.findByMerchantUid(merchantUid);
    }

    /**
     * 결제 성공 후 상태 업데이트 (PAID 상태로)
     * PointChargeRequestVO의 상태를 PAID로 바꾸고 impUid도 함께 저장
     */
    @Transactional
    public void updateStatusToPaid(PointChargeRequestVO request) {
        pointChargeRequestRepository.updateStatusAndImpUid(request);
    }

    // 필요 시 FAILED 상태 처리도 추가 가능





}
