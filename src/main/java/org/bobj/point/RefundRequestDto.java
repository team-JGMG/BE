package org.bobj.point;


import java.math.BigDecimal;
import javax.validation.constraints.Min;
import lombok.Getter;
import javax.validation.constraints.NotNull;


@Getter
public class RefundRequestDto {

    @NotNull(message = "환급 금액은 필수입니다.")
    @Min(value = 1, message = "환급 금액은 최소 1원 이상이어야 합니다.")
    private BigDecimal amount;
}
