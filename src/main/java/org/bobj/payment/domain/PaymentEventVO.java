package org.bobj.payment.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEventVO {
    private Long id;
    private  String merchantUid; // 멱등키 구성요소
    private String impUid;
    private String eventType;
    private LocalDateTime pgEventAt;
    private String source;
    private String payloadJson;
    private LocalDateTime createdAt;
}
