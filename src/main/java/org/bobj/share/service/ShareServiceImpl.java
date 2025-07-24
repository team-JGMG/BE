package org.bobj.share.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.share.dto.response.ShareResponseDTO;
import org.bobj.share.mapper.ShareMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class ShareServiceImpl implements ShareService{

    private final ShareMapper shareMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ShareResponseDTO> getSharesByUserIdPaging(Long userId, int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 10; // 페이지 크기가 0이하일 경우 기본값 설정

        int offset = page * size;

        return shareMapper.findSharesByUserIdPaging(userId, offset, size);
    }

    @Override
    public int getTotalSharesCount(Long userId) {
        return shareMapper.countSharesByUserId(userId);
    }
}
