package org.bobj.point.mapper;

import org.apache.ibatis.annotations.Param;
import org.bobj.point.domain.PointChargeRequestVO;

public interface PointChargeRequestMapper {

    void insert(PointChargeRequestVO request);

    PointChargeRequestVO findByMerchantUid(@Param("merchantUid") String merchantUid);

    //성공 전이(고정): PAID + imp_uid + paid_at
    void updateStatusAndImpUid(PointChargeRequestVO request);

    //취소 전이(고정): CANCELLED
    void updateStatusToCancelled(@Param("merchantUid") String merchantUid);



}
