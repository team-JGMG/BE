package org.bobj.user.service;

import java.util.Optional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.user.domain.SocialLoginsVO;
import org.bobj.user.domain.UserVO;
import org.bobj.user.dto.AdditionalInfoDTO;
import org.bobj.user.dto.TokenDTO;
import org.bobj.user.dto.UserResponseDTO;
import org.bobj.user.mapper.UserMapper;
import org.bobj.user.security.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;

@ControllerAdvice
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserResponseDTO getUserInfo(long userId) {
        UserVO user = userMapper.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return new UserResponseDTO(user);
    }

    public UserResponseDTO getUserInfoByEmail(String email) {
        UserVO user = userMapper.findUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        return new UserResponseDTO(user);
    }

    /**
     * [로직 변경]
     * 추가 정보와 '사전 인증 토큰'의 정보를 받아 DB에 최종 등록하고,
     * 완전한 인증 토큰을 발급하는 메소드.
     * @param dto 추가 정보 DTO
     * @param preAuthClaims '사전 인증' 토큰에서 추출한 클레임 (email, nickname, provider, providerId 등)
     * @return 최종 Access/Refresh 토큰 DTO
     */
    @Transactional
    public TokenDTO registerUserAndCreateFinalToken(AdditionalInfoDTO dto, Claims preAuthClaims) {

        try {
            log.info("Pre-auth claims: {}", preAuthClaims);

            log.info("최종 회원가입 및 토큰 발급 절차 시작");

            // 1. 사전 인증 토큰에서 사용자 소셜 정보 추출
//        String email = (String) preAuthClaims.get("email");
            String email = (String) preAuthClaims.getSubject();
            String provider = (String) preAuthClaims.get("provider");
            String providerId = (String) preAuthClaims.get("providerId");

            if (email == null || provider == null || providerId == null) {
                throw new IllegalArgumentException("사전 인증 토큰의 정보가 올바르지 않습니다.");
            }

            // 2. DB에 이미 등록된 사용자인지 최종 확인
            if (Optional.ofNullable(userMapper.findSocialLoginByProviderAndProviderId(provider, providerId))
                    .orElse(Optional.empty()).isPresent()) {
                throw new IllegalStateException("이미 등록된 소셜 계정입니다. 로그인 절차를 다시 시도해주세요.");
            }

            // 3. USERS 테이블에 사용자 저장
            UserVO newUser = new UserVO();
            newUser.setEmail(email);
            newUser.setName(dto.getName());
            newUser.setNickname(dto.getNickname()); // DTO에 닉네임 필드가 있다면 사용
            newUser.setSsn(dto.getSsn());
            newUser.setPhone(dto.getPhone());
            newUser.setBankCode(dto.getBankCode());
            newUser.setAccountNumber(dto.getAccountNumber());

            userMapper.saveUser(newUser);
            log.info("USERS 테이블에 신규 사용자 저장 완료. userId: {}", newUser.getUserId());

            // 4. SOCIAL_LOGINS 테이블에 소셜 정보 저장
            saveNewSocialLogin(newUser.getUserId(), provider, providerId, preAuthClaims);

            // 5. 최종 인증 토큰 (Access/Refresh) 생성
            String finalAccessToken = jwtTokenProvider.createAccessToken(newUser.getEmail());
            String finalRefreshToken = jwtTokenProvider.createRefreshToken(newUser.getEmail());

            // 리프레시 토큰을 DB에 저장하는 로직

            log.info("최종 인증 토큰 발급 완료. email: {}", newUser.getEmail());
            return new TokenDTO(finalAccessToken, finalRefreshToken);
        } catch (Exception e) {
            log.error("회원가입 및 토큰 발급 중 오류 발생", e);
            throw e; // 필요시 커스텀 예외로 감싸도 됨
        }
    }

//    @Transactional
//    public TokenDTO registerUserAndCreateFinalToken(AdditionalInfoDTO dto, Claims claims) {
//        // Claims는 Map<String, Object>를 상속해서 직접 변환 가능
//        return registerUserAndCreateFinalToken(dto, (Map<String, Object>) claims);
//        // 만약 형변환 문제 있으면 아래처럼 복사해서 전달 가능
//        // return registerUserAndCreateFinalToken(dto, new HashMap<>(claims));
//    }

    private void saveNewSocialLogin(Long userId, String provider, String providerId, Map<String, Object> attributes) {
        try {
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


    public Optional<UserResponseDTO> findUserInfoByEmail(String email) {
        return userMapper.findUserByEmail(email).map(UserResponseDTO::new);
    }

    public Optional<UserResponseDTO> findUserInfoById(Long userId) {
        return userMapper.findUserById(userId).map(UserResponseDTO::new);
    }



}