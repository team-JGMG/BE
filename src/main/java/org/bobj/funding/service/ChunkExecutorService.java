package org.bobj.funding.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.share.domain.ShareVO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Log4j2
@Service
@RequiredArgsConstructor
public class ChunkExecutorService {
    private final ChunkInsertService chunkInsertService;

//    private final ShareFailureRepository shareFailureRepository;


    @Async("shareDistributionExecutor")
    public CompletableFuture<Void> distributeChunkAsync(List<ShareVO> chunk, int chunkIndex){
       try{
           chunkInsertService.insertChunkTransactional(chunk);
           log.info("청크 {} 주식 배분 완료(size:{})", chunkIndex, chunk.size());
       }catch (Exception e) {
           log.error("청크 {} 주식 배분 실패 - error: {}", chunkIndex, e.getMessage(), e);
//           saveFailures(chunk, e.getMessage());
       }
        return CompletableFuture.completedFuture(null);
    }

    //실패한 청크 처리 - DB에 저장할 것인지 추후 고민
//    private void saveFailures(List<ShareVO> chunk, String reason){
//        List<ShareFa>
//    }

}
