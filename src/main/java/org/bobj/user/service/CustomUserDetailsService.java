package org.bobj.user.service;

import lombok.RequiredArgsConstructor;
import org.bobj.user.mapper.UserMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // userMapper의 결과가 null일 수 있으므로, Optional.ofNullable()로 안전하게 감싸줍니다.
        return Optional.ofNullable(userMapper.findUserByEmail(username))
                .flatMap(optionalUser -> optionalUser) // 중첩된 Optional을 풀어줍니다.
                .map(user -> new User(
                        user.getEmail(),
                        "", // JWT 방식에서는 비밀번호를 사용하지 않으므로 비워둡니다.
                        Collections.emptyList() // 권한 설정 (필요 시 DB에서 조회하여 추가)
                ))
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
    }
}