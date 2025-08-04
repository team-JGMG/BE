package org.bobj.funding.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.common.dto.CustomSlice;
import org.bobj.funding.domain.FundingOrderVO;
import org.bobj.funding.domain.FundingVO;
import org.bobj.funding.dto.FundingOrderLimitDTO;
import org.bobj.funding.dto.FundingOrderUserResponseDTO;
import org.bobj.funding.event.ShareDistributionEvent;
import org.bobj.funding.mapper.FundingMapper;
import org.bobj.funding.mapper.FundingOrderMapper;
import org.bobj.share.domain.ShareVO;
import org.bobj.share.mapper.ShareMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundingOrderService {
    private final FundingOrderMapper fundingOrderMapper;
    private final FundingMapper fundingMapper;

    private final ShareDistributionService shareDistributionService;
    private final ApplicationEventPublisher eventPublisher;

    // 주문 추가
    @Transactional
    public void createFundingOrder(Long userId, Long fundingId, int shareCount) {
        FundingVO funding = fundingMapper.findByIdWithLock(fundingId);
        if (funding == null) {
            throw new IllegalArgumentException("존재하지 않는 펀딩입니다.");
        }

        int remainingShares = funding.getRemainingShares();
        if (shareCount > remainingShares) {
            throw new IllegalArgumentException("남은 주 수를 초과했습니다.");
        }

        BigDecimal sharePrice = BigDecimal.valueOf(5000);
        BigDecimal orderPrice = sharePrice.multiply(BigDecimal.valueOf(shareCount));

        /* 요기 구현해주시면 됩니다! (Point) */
        // user의 point 가져오는 api 부탁드려요!! PointMapper.findUserPoints(userId)
        BigDecimal userPoints = BigDecimal.valueOf(100000);
        if (userPoints.compareTo(orderPrice) < 0) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }

        // 주문 생성
        fundingOrderMapper.insertFundingOrder(userId, fundingId, shareCount, orderPrice);

        /* 요기 구현해주시면 됩니다! (Point) */
        // 포인트 차감 과정(추후 구현)

        // 펀딩 현재 모인 금액 증가
        fundingMapper.increaseCurrentAmount(fundingId, orderPrice);

        // 펀딩 완료 상태 체크
        BigDecimal updatedAmount = funding.getCurrentAmount().add(orderPrice);
        if (updatedAmount.compareTo(funding.getTargetAmount()) >= 0) {
            fundingMapper.markAsEnded(fundingId);
            fundingOrderMapper.markOrdersAsSuccessByFundingId(fundingId);


            eventPublisher.publishEvent(new ShareDistributionEvent(fundingId));
//            shareDistributionService.distributeSharersAsync(fundingId); // 참여자들에게 지분 삽입
        }
    }

    // 주문 취소
    @Transactional
    public void refundFundingOrder(Long orderId, Long fundingId, BigDecimal orderPrice) {
        fundingOrderMapper.refundFundingOrder(orderId);
        fundingMapper.decreaseCurrentAmount(fundingId,orderPrice);
    }

    // 내가 투자한 주문 리스트
    public CustomSlice<FundingOrderUserResponseDTO> getFundingOrderUsers(Long userId, String status, int page, int size) {
        int offset = page*size;
        String upperStatus = status.toUpperCase();

        List<FundingOrderUserResponseDTO> content = fundingOrderMapper.findFundingOrdersByUserId(userId,upperStatus,offset,size+1);
        boolean hasNext = content.size() > size;
        if (hasNext) {
            content.remove(size);
        }

        return new CustomSlice<>(content,hasNext);
    }
    // 주문 가능 정보 조회
    @Transactional(readOnly = true)
    public FundingOrderLimitDTO getFundingOrderLimit(Long userId, Long fundingId) {
        return fundingOrderMapper.findFundingOrderLimit(userId, fundingId);
    }
}
