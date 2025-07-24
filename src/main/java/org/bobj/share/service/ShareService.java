package org.bobj.share.service;


import org.bobj.share.dto.response.ShareResponseDTO;

import java.util.List;

public interface ShareService {
    List<ShareResponseDTO> getSharesByUserIdPaging(Long userId, int page, int size);
    int getTotalSharesCount(Long userId); // 전체 개수 조회
}
