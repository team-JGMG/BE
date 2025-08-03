package org.bobj.payment.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bobj.payment.domain.PaymentVO;

@Mapper
public interface PaymentMapper {


    boolean existsByImpUid(@Param("impUid") String impUid);

    void save(PaymentVO paymentVO);

    PaymentVO findByImpUid(@Param("impUid") String impUid);

}
