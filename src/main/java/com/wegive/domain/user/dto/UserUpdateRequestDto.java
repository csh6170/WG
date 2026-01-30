package com.wegive.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [DTO] 회원 정보 수정(동네 인증) 요청 데이터
 */
@Getter
@NoArgsConstructor
public class UserUpdateRequestDto {
    private String nickname;
    private Double myLat;        // 프론트에서 보낸 현재 위도
    private Double myLon;        // 프론트에서 보낸 현재 경도
    private String addressName;  // 프론트가 보낸 주소 (보안상 무시하고, 백엔드에서 다시 찾음)
}