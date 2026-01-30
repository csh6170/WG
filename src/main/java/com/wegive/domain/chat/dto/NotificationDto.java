package com.wegive.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
    private String message; // "새 채팅이 도착했습니다!"
    private Long roomId;    // 이동할 채팅방 번호
    private String senderNickname; // 말 건 사람
}