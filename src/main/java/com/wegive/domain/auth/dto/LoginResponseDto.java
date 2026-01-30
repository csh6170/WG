package com.wegive.domain.auth.dto;

import com.wegive.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

/**
 * [DTO] 백엔드 -> 프론트엔드로 로그인 성공 결과 응답
 * 내용: 사용자 기본 정보 + 액세스 토큰
 */
@Getter
@Builder
public class LoginResponseDto {
    private Long userId;        // 우리 DB의 PK
    private String nickname;
    private String email;
    private String role;        // 권한 (USER, ADMIN)
    private String accessToken; // API 요청 시 사용할 출입증

    // User 엔티티를 받아서 DTO로 변환하는 정적 메서드 (팩토리 메서드 패턴)
    public static LoginResponseDto from(User user) {
        return LoginResponseDto.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .role(user.getRole())
                .accessToken("TEMP_ACCESS_TOKEN") // 추후 JWT 유틸리티로 토큰 생성해서 대체 필요
                .build();
    }
}