package org.bobj.user.controller;

import lombok.RequiredArgsConstructor;
import org.bobj.user.dto.UserResponseDTO;
import org.bobj.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMyInfo(@AuthenticationPrincipal User userDetails) {
        String userEmail = userDetails.getUsername();
        UserResponseDTO myInfo = userService.findUserInfoByEmail(userEmail);
        return ResponseEntity.ok(myInfo);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> getUserInfo(@PathVariable Long userId) {
        UserResponseDTO userInfo = userService.findUserInfoById(userId);
        return ResponseEntity.ok(userInfo);
    }
}