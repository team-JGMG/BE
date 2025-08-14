package org.bobj.payment.repository;


import lombok.RequiredArgsConstructor;
import org.bobj.payment.domain.PaymentVO;
import org.bobj.payment.mapper.PaymentMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentRepository {
    private final PaymentMapper paymentMapper;

    public boolean existsByImpUid(String impUid){
        return paymentMapper.existsByImpUid(impUid);
    }
    public void save(PaymentVO paymentVO){
        paymentMapper.save(paymentVO);
    }
    public PaymentVO findByImpUid(String impUid){
        return paymentMapper.findByImpUid(impUid);
    }
    public void update(PaymentVO paymentVO){
        paymentMapper.update(paymentVO);
    }
}
