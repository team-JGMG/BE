package org.bobj.funding.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.funding.domain.FundingOrderVO;
import org.bobj.funding.mapper.FundingOrderMapper;
import org.bobj.share.domain.ShareVO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Log4j2
@Service
@RequiredArgsConstructor
public class ShareDistributionService {

    private final FundingOrderMapper fundingOrderMapper;
    private final ChunkExecutorService chunkExeucutorService;

    private static final int CHUNK_SIZE = 1000;

   @Async("shareDistributionExecutor")
    public void distributeSharersAsync(Long fundingId) {
       long start = System.currentTimeMillis(); // 시작 시간 측정
       log.info("주식 배분 시작 - fundingId: {}", fundingId);

        try {
            //펀딩에 참여자 펀딩 주문 조회
            List<FundingOrderVO> orders = fundingOrderMapper.findAllOrdersByFundingId(fundingId);

            log.debug("총 {} 명의 참가자", orders.size());

            List<ShareVO> shares = orders.stream()
                    .map(order -> ShareVO.builder()
                            .userId(order.getUserId())
                            .fundingId(order.getFundingId())
                            .shareCount(order.getShareCount())
                            .averageAmount(order.getOrderPrice().divide(BigDecimal.valueOf(order.getShareCount())))
                            .build())
                    .toList();

            List<List<ShareVO>> chunks = partition(shares, CHUNK_SIZE);
            log.info("총 {} 개의 청크로 분할 완료", chunks.size());


            List<CompletableFuture<Void>> futures = new ArrayList<>();


            for(int i=0;i<chunks.size();i++){
                List<ShareVO> chunk = chunks.get(i);

                log.debug("청크 {} 시작 (size:{})", i+1, chunk.size());

//                chunkExeucutorService.distributeChunkAsync(chunk, i+1);
                futures.add(chunkExeucutorService.distributeChunkAsync(chunk, i + 1));
            }

            // 모든 비동기 작업 완료 대기
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        }catch(Exception e){
            log.error("주식 배분 중 치명적인 오류 발생 - fundingId :{}, error: {}", fundingId, e.getMessage(), e);
        } finally {
            long end = System.currentTimeMillis();
            log.info("✅ 지분 분배 완료 - 총 처리 시간: {} ms", (end - start));
        }

    }

    private <T> List<List<T>> partition(List<T> list, int size) { //청크 단위로 펀딩 주문 데이터 분할
        List<List<T>> partitions = new ArrayList<>();

        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }

        return partitions;
    }

}
