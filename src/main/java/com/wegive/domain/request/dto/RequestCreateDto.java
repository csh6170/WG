package com.wegive.domain.request.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [DTO] 나눔 신청 요청 데이터
 * 역할: 사용자가 "이 물건 신청할래요!" 할 때 보낼 데이터
 */
@Getter
@NoArgsConstructor
public class RequestCreateDto {
    private Long itemId; // 신청하려는 상품 ID
}