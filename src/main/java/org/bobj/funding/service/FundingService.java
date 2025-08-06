package org.bobj.funding.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.common.dto.CustomSlice;
import org.bobj.funding.domain.FundingOrderVO;
import org.bobj.funding.dto.FundingDetailResponseDTO;
import org.bobj.funding.dto.FundingEndedResponseDTO;
import org.bobj.funding.dto.FundingTotalResponseDTO;
import org.bobj.funding.mapper.FundingMapper;
import org.bobj.funding.mapper.FundingOrderMapper;
import org.bobj.point.service.PointService;
import org.bobj.notification.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundingService {
    private final FundingMapper fundingMapper;
    private final FundingOrderMapper fundingOrderMapper;
    private final NotificationService notificationService;
    private final PointService pointService;

    private static final int BATCH_SIZE = 1000;

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
    public void expireFunding() {
        // 펀딩 실패인 펀딩 ID 리스트 생성
        List<Long> failedFundingIds = fundingMapper.findFailedFundingIds();

        log.info("펀딩 실패 처리 대상 ID 목록: {}", failedFundingIds);


        if (failedFundingIds.isEmpty()) return;

        // 펀딩 staus FAILED로 변경
        fundingMapper.updateFundingStatusToFailed(failedFundingIds);

        // 멀티 스레드 설정
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Callable<Void>> tasks = new ArrayList<>();

        // 펀딩 ID에 해당하는 펀딩 주문들 staus를 REFUNED로 변경
        for (Long fId : failedFundingIds) {

            // 모든 주문 정보를 한 번에 가져옴
            List<FundingOrderVO> allOrders = fundingOrderMapper.findAllOrdersByFundingId(fId);

            // 1. 알림 보내기 로직
            sendFundingFailureNotifications(fId, allOrders); // 알림 메서드에 주문 리스트를 넘겨줌

            List<Long> fundingOrderIds = allOrders.stream()
                    .map(FundingOrderVO::getOrderId)
                    .toList();

            if (allOrders.isEmpty()) continue;

            for (int i = 0; i < fundingOrderIds.size(); i += BATCH_SIZE) {
                List<Long> fundingOrderIdBatch = fundingOrderIds.subList(i, Math.min(i + BATCH_SIZE, fundingOrderIds.size()));
                tasks.add(() -> {
                    fundingOrderMapper.updateFundingOrderStatusToRefundedByOrderIds(fundingOrderIdBatch);

                    /* 여기에 포인트 환불 로직 추가 해주세요!(point테이블)*/
                    for (FundingOrderVO order : allOrders) {
                        if (fundingOrderIdBatch.contains(order.getOrderId())) {
                            pointService.refundForFundingFailure(order.getUserId(), order.getOrderPrice());
                        }
                    }
                    return null;
                });
            }
        }

        try{
            List<Future<Void>> futures = executor.invokeAll(tasks);

            for(Future<Void> future: futures){
                future.get();
            }

            for (Long fId : failedFundingIds) {
                fundingMapper.updateCurrentAmountToZero(fId);
            }
        } catch(Exception e){
            log.error("펀딩 주문 상태 변경 또는 포인트 환불 중 실패 → 전체 롤백됩니다.", e);
            throw new RuntimeException("펀딩 실패 처리 중 오류", e);
        } finally {
            executor.shutdown();
        }
    }

    public void soldProperty(){

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

    public String getPropertyTitleByFundingId(Long fundingId){
        return fundingMapper.getPropertyTitleByFundingId(fundingId);
    }

    private void sendFundingFailureNotifications(Long fundingId, List<FundingOrderVO> allOrders) {
        String propertyTitle = getPropertyTitleByFundingId(fundingId);

        // 1. 등록자 알림
        Long ownerUserId = fundingMapper.getUserIdbyFundingId(fundingId);

        if (ownerUserId != null) {
            String title = "펀딩이 실패되었어요!";
            String body = "'" + propertyTitle + "' 펀딩이 목표 달성 실패로 마감되었습니다.";
            notificationService.sendNotificationAndSave(ownerUserId, title, body);
        }

        // 2. 참여자 알림
        if (!allOrders.isEmpty()) {
            List<Long> participantUserIds = allOrders.stream()
                    .map(FundingOrderVO::getUserId)
                    .distinct()
                    .toList();

            String title = "펀딩이 실패되었어요!";
            String body = "'" + propertyTitle + "' 펀딩이 목표 금액을 달성하지 못해 마감되었습니다. "
                    + "결제 포인트는 순차적으로 환불 처리되며, 환불 완료까지 시간이 소요될 수 있습니다.";
            for (Long participantId : participantUserIds) {
                notificationService.sendNotificationAndSave(participantId, title, body);
            }
        }
    }
}
