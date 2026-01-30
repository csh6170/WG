package com.wegive.domain.auth.controller;

import com.wegive.domain.auth.dto.LoginResponseDto;
import com.wegive.domain.auth.dto.SocialLoginRequestDto;
import com.wegive.domain.auth.dto.TokenRequestDto;
import com.wegive.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * [Controller] 인증 관련 API 엔드포인트
 * 주소: http://localhost:8080/api/auth/...
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService; // 로직 실행을 위해 주입

    // 로그인 요청 (POST)
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> socialLogin(@RequestBody SocialLoginRequestDto requestDto) {
        return ResponseEntity.ok(authService.processLogin(requestDto)); // Service 호출 결과 반환
    }

    // 로그아웃 요청 (POST) - URL 파라미터로 userId 받음
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestParam Long userId) {
        authService.logout(userId);
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }

    // 토큰 재발급 요청 (POST)
    @PostMapping("/reissue")
    public ResponseEntity<LoginResponseDto> reissue(@RequestBody TokenRequestDto requestDto) {
        return ResponseEntity.ok(authService.reissue(requestDto));
    }
}