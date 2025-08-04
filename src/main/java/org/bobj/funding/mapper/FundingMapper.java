package org.bobj.funding.mapper;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bobj.funding.domain.FundingOrderVO;
import org.bobj.funding.domain.FundingVO;
import org.bobj.funding.dto.FundingDetailResponseDTO;
import org.bobj.funding.dto.FundingEndedResponseDTO;
import org.bobj.funding.dto.FundingSoldResponseDTO;
import org.bobj.funding.dto.FundingTotalResponseDTO;

@Mapper
public interface FundingMapper {
    // 펀딩 상세 조회
    FundingDetailResponseDTO findFundingById(@Param("fundingId") Long fundingId);

    // 펀딩 모집 페이지에서 펀딩 리스트 조회
    List<FundingTotalResponseDTO> findTotal(
            @Param("category") String category,
            @Param("sort") String sort,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    // 펀딩 성공한 펀딩 리스트 조회
    List<FundingEndedResponseDTO> findEndedFundingProperties(@Param("offset") int offset, @Param("limit") int limit);

    // 펀딩 생성
    void insertFunding(@Param("propertyId") Long propertyId);

    // 비관적 락을 이용한 펀딩 정보 조회
    FundingVO findByIdWithLock(@Param("fundingId") Long fundingId);
    // 일반 펀딩 정보 조회
    FundingVO findById(@Param("fundingId") Long fundingId);
    //  목표 금액 도달 시 상태 ENDED + 마감 날짜 현재로 바꾸는 처리
    void markAsEnded(@Param("fundingId") Long fundingId);

    // 펀딩 모집 금액 증가
    void increaseCurrentAmount(@Param("fundingId") Long fundingId, @Param("orderPrice") BigDecimal orderPrice);

    // 펀딩 모집 금액 감소
    void decreaseCurrentAmount(@Param("fundingId") Long fundingId, @Param("orderPrice") BigDecimal orderPrice);

    // 펀딩 완료 처리
    void expireFunding(@Param("fundingId") Long fundingId);

    // 펀딩 실패인 펀딩 ID 조회
    List<Long> findFailedFundingIds();

    // 펀딩 stauts FAILD로 변경
    void updateFundingStatusToFailed(@Param("fundingIds") List<Long> fundingIds);

    // 펀딩 완료 후 2년된 매물 찾기
    List<FundingSoldResponseDTO> findSoldFundingIds();

    List<Long> findAllFundingIds();
}
