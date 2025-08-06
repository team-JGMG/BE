package org.bobj.payment.dto;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "결제 검증 요청 DTO")
public class VerifyRequestDto {

    @ApiModelProperty(value = "포트원 imp_uid", example = "imp_123456789012", required = true)
    private String impUid;

    @ApiModelProperty(value = "가맹점 고유 주문번호 (merchant_uid)", example = "point_20250804001510_1_fba1d505", required = true)
    private String merchantUid;

    @ApiModelProperty(value = "결제 금액", example = "5000", required = true)
    private BigDecimal amount;

}
