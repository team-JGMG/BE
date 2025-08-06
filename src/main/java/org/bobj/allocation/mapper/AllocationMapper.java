package org.bobj.allocation.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bobj.allocation.domain.AllocationVO;
import org.bobj.allocation.dto.AllocationResponseDTO;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AllocationMapper {

    /**
     * 특정 펀딩의 배당금 내역 조회
     */
    List<AllocationResponseDTO> findAllocationsByFundingId(@Param("fundingId") Long fundingId);

    /**
     * 배당금 생성
     */
    void insertAllocation(AllocationVO allocation);

    /**
     * 지급일이 도래한 PENDING 상태의 배당금 조회
     */
    List<AllocationVO> findPendingAllocationsForPayment(@Param("paymentDate") LocalDate paymentDate);

    /**
     * 배당금 상태 업데이트
     */
    void updateAllocationStatus(@Param("allocationsId") Long allocationsId, @Param("paymentStatus") String paymentStatus);

    /**
     * 펀딩 ID로 임대수익과 총 주식 수 조회 (배당금 계산용)
     */
    AllocationVO findRentalIncomeAndTotalShares(@Param("fundingId") Long fundingId);

    /**
     * 펀딩이 거래가능 상태가 된 날짜 조회
     */
    LocalDate findFundingEndDate(@Param("fundingId") Long fundingId);

    /**
     * 특정 펀딩의 다음 배당 예정일 조회 (가장 최근 배당일 + 1개월)
     */
    LocalDate findNextPaymentDate(@Param("fundingId") Long fundingId);
}
