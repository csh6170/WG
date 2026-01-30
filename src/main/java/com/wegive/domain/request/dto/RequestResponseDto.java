package com.wegive.domain.request.dto;

import com.wegive.domain.request.entity.Request;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * [DTO] 나눔 신청 정보 응답 데이터
 * 역할: 화면에 보여줄 신청 정보 (누가, 언제, 어떤 상태인지)
 */
@Getter
@Builder
public class RequestResponseDto {

    private Long reqId;             // 신청 ID
    private Long itemId;            // 상품 ID
    private String itemTitle;       // 상품 제목 (화면 표시용)

    private Long buyerId;           // 신청자 ID
    private String buyerNickname;   // 신청자 닉네임

    private String reqStatus;       // 상태 (WAITING, ACCEPTED, REJECTED)
    private LocalDateTime reqTime;  // 신청 시각 (0.001초 단위)

    // Entity -> DTO 변환 메서드
    public static RequestResponseDto from(Request request) {
        return RequestResponseDto.builder()
                .reqId(request.getReqId())
                .itemId(request.getItem().getItemId())
                .itemTitle(request.getItem().getTitle()) // N+1 문제 발생 가능하지만, 일단 간단히 구현
                .buyerId(request.getBuyer().getUserId())
                .buyerNickname(request.getBuyer().getNickname())
                .reqStatus(request.getReqStatus())
                .reqTime(request.getReqTime())
                .build();
    }
}