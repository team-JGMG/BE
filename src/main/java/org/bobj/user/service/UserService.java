package org.bobj.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.user.domain.SocialLoginsVO;
import org.bobj.user.domain.UserVO;
import org.bobj.user.dto.request.UserRegistrationRequestDTO;
import org.bobj.user.dto.response.AuthResponseDTO;
import org.bobj.user.dto.response.UserResponseDTO;
import org.bobj.user.mapper.UserMapper;
import org.bobj.user.security.JwtTokenProvider;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    /**
     * AuthController에서 소셜 계정 존재 여부를 확인하기 위해 사용합니다.
     */
    public Optional<SocialLoginsVO> findSocialLoginByProviderAndProviderId(String provider, String providerId) {
        log.debug("소셜 로그인 정보 조회: provider={}, providerId={}", provider, providerId);
        
        // UserMapper에서 null을 반환할 수 있으므로 Optional로 감싸기
        SocialLoginsVO result = userMapper.findSocialLoginByProviderAndProviderId(provider, providerId);
        
        if (result != null) {
            log.debug("소셜 로그인 정보 조회 성공: userId={}", result.getUserId());
            return Optional.of(result);
        } else {
            log.debug("소셜 로그인 정보를 찾을 수 없음: provider={}, providerId={}", provider, providerId);
            return Optional.empty();
        }
    }

    /**
     * 기존 소셜 계정 사용자의 로그인을 처리하고 최종 토큰을 발급합니다.
     */
    @Transactional
    public AuthResponseDTO loginExistingSocialUser(String provider, String providerId) {
        log.info("기존 소셜 계정 로그인 처리 시작: provider={}, providerId={}", provider, providerId);

        try {
            log.info("소셜 로그인 정보 조회 시작: provider={}, providerId={}", provider, providerId);
            SocialLoginsVO socialLogin = findSocialLoginByProviderAndProviderId(provider, providerId)
                    .orElseThrow(() -> new IllegalStateException("해당 소셜 계정으로 등록된 사용자가 없습니다."));
            log.info("소셜 로그인 정보 조회 성공: socialId={}, userId={}", socialLogin.getSocialId(), socialLogin.getUserId());

            log.info("사용자 정보 조회 시작: userId={}", socialLogin.getUserId());
            UserVO user = userMapper.findUserById(socialLogin.getUserId());
            if (user == null) {
                throw new IllegalStateException("연결된 사용자 계정을 찾을 수 없습니다.");
            }
            log.info("사용자 정보 조회 성공: email={}, nickname={}", user.getEmail(), user.getNickname());

            // isAdmin 정보를 AccessToken 생성 시 넘김
            log.info("JWT 토큰 생성 시작: email={}, userId={}, isAdmin={}", user.getEmail(), user.getUserId(), user.getIsAdmin());
            String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getUserId(), user.getIsAdmin());
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());
            log.info("JWT 토큰 생성 성공");

            log.info("Refresh Token DB 저장 시작");
            socialLogin.setRefreshToken(refreshToken);
            userMapper.updateRefreshToken(socialLogin);
            log.info("Refresh Token DB 저장 완료. userId: {}", user.getUserId());

            log.info("기존 소셜 계정 로그인 처리 완료");
            return new AuthResponseDTO(accessToken, refreshToken, user.getUserId(), user.getIsAdmin());
        } catch (Exception e) {
            log.error("기존 소셜 계정 로그인 처리 중 예외 발생: provider={}, providerId={}", provider, providerId, e);
            throw e;
        }
    }

    /**
     * 추가 정보와 '사전 인증 토큰'의 정보를 받아 DB에 최종 등록하고,
     * 완전한 인증 토큰을 발급하는 메소드.
     */
    @Transactional
    public AuthResponseDTO registerUserAndCreateFinalToken(UserRegistrationRequestDTO dto, Claims preAuthClaims) {
        log.info("최종 회원가입 및 토큰 발급 절차 시작");

        String email = preAuthClaims.getSubject();
        String provider = preAuthClaims.get("provider", String.class);
        String providerId = preAuthClaims.get("providerId", String.class);
        String kakaoNickname = preAuthClaims.get("nickname", String.class); // 카카오 닉네임 추출

        if (email == null || provider == null || providerId == null) {
            throw new IllegalArgumentException("사전 인증 토큰의 정보가 올바르지 않습니다.");
        }

        if (findSocialLoginByProviderAndProviderId(provider, providerId).isPresent()) {
            throw new IllegalStateException("이미 등록된 소셜 계정입니다. 로그인 절차를 다시 시도해주세요.");
        }

        UserVO newUser = new UserVO();
        newUser.setEmail(email);
        newUser.setName(dto.getName());
        newUser.setNickname(kakaoNickname); // 카카오에서 받은 닉네임 설정
        newUser.setSsn(dto.getSsn());
        newUser.setPhone(dto.getPhone());
        newUser.setBankCode(dto.getBankCode());
        newUser.setAccountNumber(dto.getAccountNumber());

        userMapper.saveUser(newUser);
        log.info("USERS 테이블에 신규 사용자 저장 완료. userId: {}", newUser.getUserId());

        saveNewSocialLogin(newUser.getUserId(), provider, providerId, preAuthClaims);

        String finalAccessToken = jwtTokenProvider.createAccessToken(newUser.getEmail(), newUser.getUserId(), newUser.getIsAdmin());
        String finalRefreshToken = jwtTokenProvider.createRefreshToken(newUser.getEmail());
        // (선택) 발급된 Refresh Token을 DB에 바로 저장
        SocialLoginsVO savedSocialLogin = findSocialLoginByProviderAndProviderId(provider, providerId).get();
        savedSocialLogin.setRefreshToken(finalRefreshToken);
        userMapper.updateRefreshToken(savedSocialLogin);

        log.info("최종 인증 토큰 발급 완료. email: {}", newUser.getEmail());
        return new AuthResponseDTO(finalAccessToken, finalRefreshToken, newUser.getUserId(), newUser.getIsAdmin());    }

    private void saveNewSocialLogin(Long userId, String provider, String providerId, Claims attributes) {
        try {
            // Claims 객체는 Map과 호환되므로 직접 변환할 수 있습니다.
            String profileDataJson = objectMapper.writeValueAsString(attributes);
            SocialLoginsVO newSocialLogin = SocialLoginsVO.builder()
                    .userId(userId)
                    .provider(provider)
                    .providerId(providerId)
                    .profileData(profileDataJson)
                    .build();
            userMapper.saveSocialLogin(newSocialLogin);
        } catch (JsonProcessingException e) {
            log.error("소셜 프로필 JSON 변환에 실패했습니다.", e);
            throw new RuntimeException("프로필 데이터 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 이메일로 사용자 조회 (UserVO 반환)
     */
    public UserVO findUserVOByEmail(String email) {
        UserVO user = userMapper.findUserByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("유저를 찾을 수 없습니다: " + email);
        }
        return user;
    }

    /**
     * 사용자 ID로 사용자 조회 (UserVO 반환)
     */
    public UserVO findUserVOById(Long userId) {
        UserVO user = userMapper.findUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("해당 ID의 사용자를 찾을 수 없습니다: " + userId);
        }
        return user;
    }

    /**
     * 이메일로 사용자 정보 조회 (DTO 반환)
     */
    public UserResponseDTO findUserInfoByEmail(String email) {
        UserVO user = findUserVOByEmail(email);
        return new UserResponseDTO(user);
    }

    /**
     * 사용자 ID로 사용자 정보 조회 (DTO 반환)
     */
    public UserResponseDTO findUserInfoById(Long userId) {
        UserVO user = findUserVOById(userId);
        return new UserResponseDTO(user);
    }


    /**
     * 이메일로 Refresh Token을 조회하여 새로운 Access Token 발급
     * 클라이언트가 Refresh Token을 모르는 경우 사용
     */
    @Transactional
    public AuthResponseDTO refreshAccessTokenByEmail(String email) {
        log.info("이메일로 토큰 갱신 시도: {}", email);
        
        // 사용자 정보 조회
        UserVO user = userMapper.findUserByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email);
        }
        
        // 소셜 로그인 정보에서 Refresh Token 조회
        SocialLoginsVO socialLogin = userMapper.findSocialLoginByUserId(user.getUserId());
        if (socialLogin == null || socialLogin.getRefreshToken() == null) {
            throw new IllegalArgumentException("Refresh Token을 찾을 수 없습니다: " + email);
        }
        
        // Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(socialLogin.getRefreshToken())) {
            log.warn("Refresh Token이 만료됨: {}", email);
            // 만료된 Refresh Token 제거
            removeRefreshToken(email);
            throw new IllegalArgumentException("Refresh Token이 만료되었습니다. 다시 로그인해주세요.");
        }
        
        // 새로운 Access Token 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getUserId(), user.getIsAdmin());
        
        log.info("토큰 갱신 성공: {}", email);
        return new AuthResponseDTO(newAccessToken, socialLogin.getRefreshToken(), user.getUserId(), user.getIsAdmin());
    }

    /**
     * 로그아웃 시 Refresh Token 제거
     */
    @Transactional
    public void removeRefreshToken(String email) {
        UserVO user = userMapper.findUserByEmail(email);
        if (user != null) {
            userMapper.clearRefreshTokenByUserId(user.getUserId());
        }
    }

    /**
     * 사용자 ID로 소셜 로그인 정보 조회 (토큰 갱신용)
     */
    public SocialLoginsVO findSocialLoginByUserId(Long userId) {
        return userMapper.findSocialLoginByUserId(userId);
    }

}
