package org.bobj.point.mapper;

import org.apache.ibatis.annotations.Param;
import org.bobj.point.domain.PointChargeRequestVO;

public interface PointChargeRequestMapper {

    void insert(PointChargeRequestVO request);

    PointChargeRequestVO findByMerchantUid(@Param("merchantUid") String merchantUid);

    void updateStatusAndImpUid(PointChargeRequestVO request);
}
