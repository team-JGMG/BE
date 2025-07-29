package org.bobj.funding.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.common.dto.CustomSlice;
import org.bobj.funding.dto.FundingDetailResponseDTO;
import org.bobj.funding.dto.FundingEndedResponseDTO;
import org.bobj.funding.dto.FundingTotalResponseDTO;
import org.bobj.funding.mapper.FundingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundingService {
    private final FundingMapper fundingMapper;

    public CustomSlice<FundingTotalResponseDTO> getFundingList(String category, String sort, int page, int size) {
        int offset = page*size;
        List<FundingTotalResponseDTO> content = fundingMapper.findTotal(category, sort, offset, size+1);

        boolean hasNext = content.size() > size;
        if(hasNext) {
            content.remove(size);
        }

        return new CustomSlice<>(content,hasNext);
    }

    @Transactional
    public void expireFunding(Long fundingId) {
        fundingMapper.expireFunding(fundingId);
    }

    public FundingDetailResponseDTO getFundingDetail(Long fundingId) {
        return fundingMapper.findFundingById(fundingId);
    }

    public CustomSlice<FundingEndedResponseDTO> getEndedFundingProperties(int page, int size) {
        int offset = page * size;
        List<FundingEndedResponseDTO> content = fundingMapper.findEndedFundingProperties(offset,size+1);

        boolean hasNext = content.size() > size;
        if(hasNext){
            content.remove(size);
        }

        return new CustomSlice<>(content,hasNext);
    }
}
