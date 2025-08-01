package org.bobj.funding.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.share.domain.ShareVO;
import org.bobj.share.mapper.ShareMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class ChunkInsertService { // 청크 크기로 나누어진 ShareVO 리스트 DB에 삽입 - 트랜잭션

    private final ShareMapper shareMapper;

    @Transactional
    public void insertChunkTransactional(List<ShareVO> chunk){
        try{
            int inserted = shareMapper.insertSharesBatch(chunk);
            log.debug("DB에 {}건 insert 성공", inserted);
        }catch(Exception e){
            log.error("DB insert 중 예외 발생 - 청크 크기 : {}, 에러 {}", chunk.size(), e.getMessage(), e);
            throw e;
        }
    }
}
