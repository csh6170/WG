package com.wegive.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [DTO] 프론트엔드 -> 백엔드로 로그인 요청 시 받는 데이터
 * 시점: 사용자가 '카카오 로그인' 버튼을 누르고 인증이 완료된 직후
 */
@Getter
@NoArgsConstructor
public class SocialLoginRequestDto {
    private String email;       // 소셜에서 받은 이메일 (회원 식별용)
    private String nickname;    // 소셜 닉네임
    private String profileImg;  // 프로필 사진 URL
    private String provider;    // 가입 경로 (KAKAO, NAVER, GOOGLE)
    private String providerId;  // 소셜 고유 ID (비밀번호 대체)
}