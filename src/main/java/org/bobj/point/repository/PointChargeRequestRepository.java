package org.bobj.point.repository;

import lombok.RequiredArgsConstructor;
import org.bobj.point.domain.PointChargeRequestVO;
import org.bobj.point.mapper.PointChargeRequestMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointChargeRequestRepository {
    private final PointChargeRequestMapper mapper;

    public void save(PointChargeRequestVO request) {
        mapper.insert(request);
    }

    public PointChargeRequestVO findByMerchantUid(String merchantUid) {
        return mapper.findByMerchantUid(merchantUid);
    }

    public void updateStatusAndImpUid(PointChargeRequestVO request) {
        mapper.updateStatusAndImpUid(request);
    }

    //취소 전이
    public void updateStatusToCancelled(String merchantUid) {
        mapper.updateStatusToCancelled(merchantUid);
    }

}
