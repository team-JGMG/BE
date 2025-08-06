package org.bobj.funding.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundingEndedResponseDTO {
    @ApiModelProperty("펀딩 ID")
    private Long fundingId;

    @ApiModelProperty("매물 제목")
    private String title;

    @ApiModelProperty("해시태그 리스트")
    private List<String> tags;
}
