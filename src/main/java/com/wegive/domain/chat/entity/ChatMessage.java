package com.wegive.domain.chat.entity;

import com.wegive.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender; // 보낸 사람

    @Column(nullable = false)
    private String message;

    private LocalDateTime sendTime;

    // 🔴 추가: 0은 안읽음, 1은 읽음
    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private int isRead = 0;
}