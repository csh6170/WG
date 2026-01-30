package com.wegive.domain.auth.service;

import com.wegive.domain.auth.dto.LoginResponseDto;
import com.wegive.domain.auth.dto.SocialLoginRequestDto;
import com.wegive.domain.auth.dto.TokenRequestDto;
import com.wegive.domain.user.entity.User;
import com.wegive.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [Service] 로그인, 로그아웃, 토큰 재발급 비즈니스 로직 담당
 */
@Service
@RequiredArgsConstructor
@Transactional // 모든 메서드 실행 중 에러 발생 시 DB 롤백 보장
public class AuthService {

    private final UserRepository userRepository; // DB 작업을 위해 필요

    /**
     * 기능: 소셜 로그인 처리 (회원가입 or 로그인 판별)
     * 로직: 이메일로 DB 조회 -> 있으면 로그인, 없으면 회원가입(registerUser)
     */
    public LoginResponseDto processLogin(SocialLoginRequestDto dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseGet(() -> registerUser(dto)); // Optional: 값이 없으면 registerUser 실행
        return LoginResponseDto.from(user);
    }

    /**
     * 기능: 신규 회원가입 (내부 호출용)
     * 로직: DTO 정보를 User 엔티티로 변환 후 DB 저장
     */
    private User registerUser(SocialLoginRequestDto dto) {
        User newUser = User.builder()
                .email(dto.getEmail())
                .nickname(dto.getNickname())
                .provider(dto.getProvider())
                .providerId(dto.getProviderId())
                .build();
        return userRepository.save(newUser); // INSERT 쿼리 실행
    }

    /**
     * 기능: 로그아웃
     * 로직: DB에 저장된 리프레시 토큰을 삭제하여 재발급 불가능하게 만듦
     */
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("회원 없음"));
        user.updateRefreshToken(null); // 더티 체킹(Dirty Checking)으로 UPDATE 쿼리 발생
    }

    /**
     * 기능: 토큰 재발급
     * 로직: 리프레시 토큰이 DB에 있는지 확인 후, 있으면 새 정보 반환
     */
    public LoginResponseDto reissue(TokenRequestDto requestDto) {
        User user = userRepository.findByRefreshToken(requestDto.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("유효하지 않은 토큰")); // 토큰 탈취/만료 감지
        return LoginResponseDto.from(user);
    }
}