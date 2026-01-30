package com.wegive.domain.chat.dto;

import com.wegive.domain.chat.entity.ChatRoom;
import com.wegive.domain.item.entity.Item;
import com.wegive.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoomListDto {
    private Long roomId;
    private Item item;      // 상품 정보 (이미지, 제목용)
    private User seller;    // 나눔이
    private User buyer;     // 나눔 받는이
    private String lastMessage;      // 마지막 메시지 내용
    private LocalDateTime lastTime;  // 마지막 메시지 시간
    private Long unreadCount; // 안 읽은 메시지 수 추가

    // 엔티티 -> DTO 변환 메서드
    public static ChatRoomListDto of(ChatRoom room, String lastMessage, LocalDateTime lastTime,Long unreadCount) {
        return ChatRoomListDto.builder()
                .roomId(room.getRoomId())
                .item(room.getItem())
                .seller(room.getSeller())
                .buyer(room.getBuyer())
                .lastMessage(lastMessage)
                .lastTime(lastTime)
                .unreadCount(unreadCount) // 개수 세팅
                .build();
    }
}