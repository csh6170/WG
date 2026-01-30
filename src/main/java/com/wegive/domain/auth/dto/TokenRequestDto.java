package com.wegive.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [DTO] 액세스 토큰 만료 시 재발급 요청 데이터
 * 사용: 프론트엔드가 401 에러(로그인 풀림)를 받았을 때 이 DTO를 보냄
 */
@Getter
@NoArgsConstructor
public class TokenRequestDto {
    private String refreshToken; // "나 인증된 사람이니까 다시 문 열어줘" 증명서
}