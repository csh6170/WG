package com.wegive.domain.chat.service;

import com.wegive.domain.chat.dto.ChatMessageDto;
import com.wegive.domain.chat.dto.ChatRoomListDto;
import com.wegive.domain.chat.entity.ChatMessage;
import com.wegive.domain.chat.entity.ChatRoom;
import com.wegive.domain.chat.repository.ChatMessageRepository;
import com.wegive.domain.chat.repository.ChatRoomRepository;
import com.wegive.domain.item.entity.Item;
import com.wegive.domain.item.repository.ItemRepository;
import com.wegive.domain.user.entity.User;
import com.wegive.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository; // 필드 추가 (생성자 주입 확인!)

    /**
     * 채팅방 생성 또는 조회
     * (이미 존재하는 방이면 그 방 ID를 반환, 없으면 새로 생성)
     */
    public Long createChatRoom(Long itemId, Long buyerId) {
        Item item = itemRepository.findById(itemId).orElseThrow();
        User buyer = userRepository.findById(buyerId).orElseThrow();

        // 1. 이미 존재하는 방인지 확인
        return chatRoomRepository.findByItemAndBuyer(item, buyer)
                .map(ChatRoom::getRoomId)
                .orElseGet(() -> {
                    // 2. 없으면 새로 생성
                    ChatRoom room = ChatRoom.builder()
                            .item(item)
                            .seller(item.getSeller())
                            .buyer(buyer)
                            .build();
                    chatRoomRepository.save(room);
                    return room.getRoomId();
                });
    }
    /**
     * [메시지 저장]
     * 1. DB에 저장
     * 2. DTO로 변환하여 반환 (컨트롤러가 구독자들에게 쏘기 위해)
     */
    public ChatMessageDto saveMessage(ChatMessageDto dto) {
        ChatRoom room = chatRoomRepository.findById(dto.getRoomId()).orElseThrow();
        User sender = userRepository.findById(dto.getSenderId()).orElseThrow();

        // 1. DB 저장
        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .message(dto.getMessage())
                .sendTime(LocalDateTime.now())
                .build();
        chatMessageRepository.save(message);

        // 2. DTO에 시간/닉네임 채워서 반환
        dto.setSendTime(message.getSendTime());
        dto.setSenderNickname(sender.getNickname());
        return dto;
    }

    // [채팅방 조회] 입장 시 필요
    @Transactional(readOnly = true)
    public ChatRoom findRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId).orElseThrow();
    }

    // [이전 대화 내역 가져오기]
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessages(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow();
        return chatMessageRepository.findByChatRoomOrderBySendTimeAsc(room).stream()
                .map(msg -> ChatMessageDto.builder()
                        .roomId(roomId)
                        .senderId(msg.getSender().getUserId())
                        .senderNickname(msg.getSender().getNickname())
                        .message(msg.getMessage())
                        .sendTime(msg.getSendTime())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * [수정] 내 채팅방 목록 조회 (+ 마지막 메시지 포함)
     */
    @Transactional(readOnly = true)
    public List<ChatRoomListDto> findAllRoom(Long userId) {
        // 1. 유저 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2. 내가 속한 채팅방 리스트 가져오기 (Repository에서 JOIN FETCH 적용됨)
        List<ChatRoom> rooms = chatRoomRepository.findBySellerOrBuyerOrderByRoomIdDesc(user, user);


        // 3. DTO로 변환하면서 마지막 메시지 채워넣기
        return rooms.stream().map(room -> {
            // 해당 방의 가장 최근 메시지 1개 조회
            ChatMessage lastMsg = chatMessageRepository.findFirstByChatRoomOrderBySendTimeDesc(room);
            Long unreadCount = chatMessageRepository.countByChatRoomAndSenderNotAndIsRead(room, user, 0);
            // 초기값 설정
            String msgContent = "대화를 시작해보세요!";
            LocalDateTime msgTime = room.getCreatedAt();

            // 마지막 메시지가 있다면 데이터 교체
            if (lastMsg != null) {
                msgContent = lastMsg.getMessage();
                msgTime = lastMsg.getSendTime();
            }

            // DTO 변환 (이미 room 내부의 item, seller, buyer는 FETCH JOIN으로 채워져 있음)
            return ChatRoomListDto.of(room, msgContent, msgTime, unreadCount);
        }).collect(Collectors.toList());
    }
    /**
     * [추가] 채팅방 입장 시 메시지 읽음 처리
     * @param roomId 채팅방 ID
     * @param userId 내 유저 ID (내가 보낸 메시지는 제외하고 읽음 처리하기 위함)
     */
    @Transactional
    public void markMessagesAsRead(Long roomId, Long userId) {
        // Repository에 추가한 @Modifying 쿼리를 실행합니다.
        chatMessageRepository.markAsRead(roomId, userId);
    }
}