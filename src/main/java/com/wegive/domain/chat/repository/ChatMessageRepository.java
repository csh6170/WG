package com.wegive.domain.chat.repository;

import com.wegive.domain.chat.entity.ChatMessage;
import com.wegive.domain.chat.entity.ChatRoom;
import com.wegive.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 채팅방의 메시지 내역 가져오기
    List<ChatMessage> findByChatRoomOrderBySendTimeAsc(ChatRoom chatRoom);
    // [추가] 특정 방의 '가장 최근 메시지 1개' 조회 (목록용)
    // SendTime 기준 내림차순(Desc) 정렬 후 맨 위 1개(First)만 가져옴
    ChatMessage findFirstByChatRoomOrderBySendTimeDesc(ChatRoom chatRoom);
    // [추가] 특정 채팅방의 모든 메시지 삭제
    void deleteByChatRoom(ChatRoom chatRoom);
    // [추가] 특정 방에서 '내가 아닌 사람(상대방)'이 보낸 안 읽은 메시지 개수 조회
    Long countByChatRoomAndSenderNotAndIsRead(ChatRoom chatRoom, User sender, int isRead);
    // 🔴 추가: 채팅방 입장 시 '상대방이 보낸 메시지'를 모두 읽음 처리
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = 1 " +
            "WHERE m.chatRoom.roomId = :roomId " +
            "AND m.sender.userId != :userId " +
            "AND m.isRead = 0")
    void markAsRead(@Param("roomId") Long roomId, @Param("userId") Long userId);
}