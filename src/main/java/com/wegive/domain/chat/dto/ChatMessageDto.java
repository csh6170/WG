package com.wegive.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDto {
    private Long roomId;    // 방 번호
    private Long senderId;  // 보낸 사람 ID
    private String message; // 메시지 내용
    private String senderNickname; // 보낸 사람 닉네임 (화면 표시용)
    private LocalDateTime sendTime; // 보낸 시간
    // [추가] 마지막 메시지 내용과 시간
    private String lastMessage;
    private LocalDateTime lastMessageTime;
}