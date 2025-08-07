package org.bobj.property.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.common.dto.CustomSlice;
import org.bobj.common.s3.S3Service;
import org.bobj.funding.dto.FundingSoldResponseDTO;
import org.bobj.funding.mapper.FundingMapper;
import org.bobj.notification.service.NotificationService;
import org.bobj.point.service.PointService;
import org.bobj.property.domain.PropertyDocumentType;
import org.bobj.property.domain.PropertyVO;
import org.bobj.property.dto.*;
import org.bobj.property.mapper.PropertyMapper;
import org.bobj.share.domain.ShareVO;
import org.bobj.share.mapper.ShareMapper;
import org.bobj.share.mapper.ShareMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyService {
    private final PropertyMapper propertyMapper;
    private final FundingMapper fundingMapper;
    private final ShareMapper shareMapper;

    private final RentalIncomeService rentalIncomeService;
    private final S3Service s3Service;
    private final NotificationService notificationService;
    private final ShareMapper shareMapper;
    private final PointService pointService;

    // 매물 승인 + 펀딩 등록 or 거절
    @Transactional
    public void updatePropertyStatus(Long propertyId, String status) {
        propertyMapper.update(propertyId,status);

        PropertyVO vo = propertyMapper.findByPropertyId(propertyId);

        Long ownerUserId = vo.getUserId();
        String propertyTitle = vo.getTitle();

        // 상태가 APPROVED일 때만 fundings 테이블에 insert
        if ("APPROVED".equalsIgnoreCase(status)) {

            //매물 승인 알림
            String title = "매물이 승인되었어요!";
            String body = "회원님의 매물(" + propertyTitle + ")이 승인되어 펀딩이 시작되었습니다.";

            notificationService.sendNotificationAndSave(ownerUserId, title, body);

            fundingMapper.insertFunding(propertyId);

        }else if ("REJECTED".equalsIgnoreCase(status)) {
            // 거절 알림
            String title = "매물이 거절되었어요!";
            String body = "회원님의 매물(" + propertyTitle + ")이 관리자 검토 후 거절되었습니다.";

            notificationService.sendNotificationAndSave(ownerUserId, title, body);
        }

    }

    // 매물 등록
    @Transactional
    public void registerProperty(PropertyCreateDTO dto,
                                 List<MultipartFile> photoFiles,
                                 List<PropertyDocumentRequestDTO> documentRequests) {
        PropertyVO vo = dto.toVO();
        
        // 매물 등록 (DB 트랜잭션 내에서 처리)
        propertyMapper.insert(vo);
        Long propertyId = vo.getPropertyId();

        if (dto.getHashTagIds() != null && !dto.getHashTagIds().isEmpty()) {
            propertyMapper.insertHashtag(propertyId, dto.getHashTagIds());
            log.info("해시태그 매핑 완료 - 매물ID: {}, 해시태그ID: {}", propertyId, dto.getHashTagIds());
        }

        // insert 후 생성된 ID 확인
        log.info("매물 등록 완료 - ID: {}, 제목: {}", vo.getPropertyId(), vo.getTitle());
        
        // 트랜잭션 커밋 후 비동기로 월세 계산 처리
        if (vo.getPropertyId() != null && vo.getRawdCd() != null && !vo.getRawdCd().trim().isEmpty()) {
            // 트랜잭션 외부에서 API 호출 처리
            calculateAndUpdateRentalIncome(vo.getPropertyId(), vo.getRawdCd(), vo.getAddress());
        } else {
            if (vo.getPropertyId() != null) {
                if (vo.getRawdCd() == null || vo.getRawdCd().trim().isEmpty()) {
                    log.warn("법정동코드가 없어서 월세 자동 계산을 건너뛰었습니다 - 매물ID: {}", vo.getPropertyId());
                }
            } else {
                log.error("매물 등록 후 ID 생성 실패 - 월세 자동 계산 불가");
            }
        }

        // 이미지 업로드 및 DB 저장
        if (photoFiles != null) {
            for (MultipartFile photo : photoFiles) {
                String photoUrl = s3Service.upload(photo, true); // public-read로 업로드
                propertyMapper.insertPropertyPhoto(propertyId, photoUrl); // DB 저장
            }
        }

        // 문서 업로드 및 DB 저장
        if (documentRequests != null) {
            for (PropertyDocumentRequestDTO request : documentRequests) {
                MultipartFile file = request.getFile();
                PropertyDocumentType type = request.getType();
                String url = s3Service.upload(file, false); // 비공개로 업로드
                propertyMapper.insertPropertyDocument(propertyId, type.name(), url);
            }
        }
    }
    
    /**
     * 트랜잭션 외부에서 월세 계산 및 업데이트 처리
     * DB 커넥션 점유 시간을 최소화하기 위해 별도 메서드로 분리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void calculateAndUpdateRentalIncome(Long propertyId, String rawdCd, String address) {
        try {
            log.info("매물 자동 월세 계산 시작 - 매물ID: {}, 법정동코드: {}, 주소: {}", propertyId, rawdCd, address);
            
            // API 호출로 월세 데이터 계산 (시간이 오래 걸릴 수 있음)
            BigDecimal calculatedRentalAmount = rentalIncomeService.getLatestMonthlyRentForProperty(rawdCd, address);
            
            if (calculatedRentalAmount != null) {
                // 새로운 트랜잭션에서 빠른 업데이트
                propertyMapper.updateRentalIncome(propertyId, calculatedRentalAmount);
                log.info("매물 월세 자동 설정 완료 - 매물ID: {}, 월세: {}원", propertyId, calculatedRentalAmount);
            } else {
                log.warn("매물 월세 자동 계산 실패 - 매물ID: {}, 매칭되는 월세 데이터 없음, 기본값 적용 안함", propertyId);
            }
            
        } catch (Exception e) {
            log.error("매물 월세 자동 계산 중 오류 발생 - 매물ID: {}, 오류: {}", propertyId, e.getMessage(), e);
            // 월세 계산 실패해도 매물 등록은 성공으로 처리 (이미 커밋됨)
        }
    }

    // 매물 리스트 조회(관리자 페이지)
    public CustomSlice<PropertyTotalDTO> getAllPropertiesByStatus(String category, int page, int size) {
        int offset = page * size;

        List<PropertyVO> vos = propertyMapper.findTotal(category, offset, size+1);
        boolean hasNext = vos.size() > size;
        if(hasNext) {
            vos.remove(size);
        }

        List<PropertyTotalDTO> dtoList = vos.stream()
                .map(PropertyTotalDTO::of)
                .collect(Collectors.toList());
        return new CustomSlice<>(dtoList,hasNext);
    }

    // 유저의 매물 리스트 조회(마이페이지)
    public CustomSlice<PropertyUserResponseDTO> getUserPropertiesByStatus(Long userId, String status, int page, int size) {
        int offset = page * size;
        String upperStatus = status.toUpperCase();

        List<PropertyUserResponseDTO> resultList =
                propertyMapper.findByUserId(userId, upperStatus, offset, size + 1);

        boolean hasNext = resultList.size() > size;
        if (hasNext) {
            resultList.remove(size);
        }

        // 펀딩 관련 계산 (APPROVED, SOLD일 때만 적용)
        if (upperStatus.equals("APPROVED") || upperStatus.equals("SOLD")) {
            for (PropertyUserResponseDTO dto : resultList) {
                if (dto.getTargetAmount() != null
                        && dto.getCurrentAmount() != null
                        && dto.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {

                    BigDecimal target = dto.getTargetAmount();
                    BigDecimal current = dto.getCurrentAmount();

                    int rate = current
                            .multiply(BigDecimal.valueOf(100))
                            .divide(target, 0, RoundingMode.DOWN)
                            .intValue();
                    dto.setAchievementRate(rate);

                    BigDecimal remainingAmount = target.subtract(current);
                    dto.setRemainingAmount(remainingAmount);

                    long remainingShares = remainingAmount
                            .divide(BigDecimal.valueOf(5000), 0, RoundingMode.DOWN)
                            .longValue();
                    dto.setRemainingShares(remainingShares);
                }
            }
        }

        return new CustomSlice<>(resultList, hasNext);
    }

    // 매물 상세 조회(관리자, 마이페이지(내가 올린 매물)
    public PropertyDetailDTO getPropertyById(Long propertyId) {
        PropertyVO vo = propertyMapper.findByPropertyId(propertyId);
        return PropertyDetailDTO.of(vo, s3Service);
    }

    // 매각 완료 매물 리스트
    public List<PropertySoldResponseDTO> getSoldProperties() {
        return propertyMapper.findSold();
    }

    // 매각 처리
    public void soldProperties(){
        List<FundingSoldResponseDTO> fundings = fundingMapper.findSoldFundingIds();
        if (fundings.isEmpty()) {
            log.info("매각 대상 없음");
            return;
        }

        List<Long> propertyIds = fundings.stream()
                .map(FundingSoldResponseDTO::getPropertyId)
                .collect(Collectors.toList());

        // 1. 매물 상태를 SOLD, updated_at, sold_at 수정 + 누적 수익률 계산
        propertyMapper.updatePropertiesAsSold(propertyIds);

        // 멀티 스레드 설정
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Callable<Void>> tasks = new ArrayList<>();

        for(FundingSoldResponseDTO dto : fundings) {
            Long fundingId = dto.getFundingId();

            // 펀딩 등록자와 참여자에게 매각 완료 알림 전송
            sendFundingSoldNotifications(fundingId);

            executor.submit(()->{
                /* fundingId에 해당하는 share 가져오기(share 테이블)*/
                List<ShareVO> shares = shareMapper.findByFundingId(fundingId);
                /* 해당하는 지분에 point 환불(point 테이블) */
                for (ShareVO share : shares) {
                    pointService.refundForShareSell(share.getUserId(),
                        BigDecimal.valueOf(5000* share.getShareCount()));
                }
            });
        }
        executor.shutdown();
    }

    /**
     * 매물 ID로 법정동 코드 조회
     */
    public String getRawdCdByPropertyId(Long propertyId) {
        log.info("매물 ID로 법정동 코드 조회 - 매물ID: {}", propertyId);

        String rawdCd = propertyMapper.findRawdCdByPropertyId(propertyId);

        if (rawdCd == null || rawdCd.trim().isEmpty()) {
            log.warn("매물 ID: {}에 해당하는 법정동 코드를 찾을 수 없습니다", propertyId);
            return null;
        }

        log.info("법정동 코드 조회 성공 - 매물ID: {}, 법정동코드: {}", propertyId, rawdCd);
        return rawdCd;
    }

    private void sendFundingSoldNotifications(Long fundingId) {
        String propertyTitle = fundingMapper.getPropertyTitleByFundingId(fundingId);

        // 1. 등록자 알림 (펀딩을 올린 사용자)
        Long ownerUserId = fundingMapper.getUserIdbyFundingId(fundingId);
        if (ownerUserId != null) {
            String title = "매각 완료!";
            String body = "'" + propertyTitle + "' 매물이 성공적으로 매각되었습니다. 수익금을 확인해 주세요.";
            notificationService.sendNotificationAndSave(ownerUserId, title, body);
        }

        // 2. 참여자 알림
        List<Long> participantUserIds = shareMapper.findShareHolderUserIdsByFundingId(fundingId);
        if (!participantUserIds.isEmpty()) {
            String title = "매각 완료!";
            String body = "'" + propertyTitle + "' 매각이 완료되어 지분 수익금이 정산되었습니다.";

            // 배치 전송 + 일괄 DB 저장
            try {
                notificationService.sendBatchNotificationsAndSave(participantUserIds, title, body);
            } catch (Exception e) {
                log.error("참여자 대상 매각 알림 전송 실패 - fundingId: {}", fundingId, e);
            }
        }
    }
}
