package com.wegive.domain.chat.controller;

import com.wegive.domain.chat.dto.ChatMessageDto;
import com.wegive.domain.chat.dto.ChatRoomListDto;
import com.wegive.domain.chat.dto.NotificationDto;
import com.wegive.domain.chat.entity.ChatRoom;
import com.wegive.domain.chat.service.ChatService;
import com.wegive.domain.item.dto.ItemResponseDto;
import com.wegive.domain.item.service.ItemService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate; // 👈 메시지 쏘는 도구
    private final ItemService itemService; // 상단 상품 정보 표시용

    // 1. 채팅방 입장 (화면)
    @GetMapping("/chat/room/{roomId}")
    public String enterRoom(@PathVariable Long roomId, Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/";

        // [추가] 입장하자마자 상대방이 보낸 메시지들을 읽음 처리
        chatService.markMessagesAsRead(roomId, userId);

        ChatRoom room = chatService.findRoomById(roomId);
        List<ChatMessageDto> messages = chatService.getMessages(roomId);

        // [수정] 상품 정보 안전하게 가져오기
        ItemResponseDto item = null;
        String roomName = "알 수 없는 채팅방"; // 기본값 설정

        try {
            item = itemService.getItemDetail(userId, room.getItem().getItemId());
            if (item != null) {
                roomName = item.getTitle(); // 상품이 있을 때만 제목 가져오기
            }
        } catch (Exception e) {
            System.out.println("상품 정보를 불러올 수 없음 (삭제됨 등): " + e.getMessage());
            roomName = "삭제된 게시글입니다"; // 예외 발생 시 제목 설정
        }

        // [추가] 상대방(Partner) ID 구하기
        Long partnerId;
        if (room.getSeller().getUserId().equals(userId)) {
            partnerId = room.getBuyer().getUserId(); // 내가 판매자면 -> 상대는 구매자
        } else {
            partnerId = room.getSeller().getUserId(); // 내가 구매자면 -> 상대는 판매자
        }

        model.addAttribute("roomId", roomId);
        model.addAttribute("roomName", roomName); // 안전한 roomName 사용
        model.addAttribute("messages", messages);
        model.addAttribute("item", item); // null일 수 있음 (HTML에서 처리 필요)
        model.addAttribute("myUserId", userId);
        // 모델에 담아서 HTML로 보냄
        model.addAttribute("partnerId", partnerId);

        return "chat/room";
    }

    // 2. 채팅방 생성 요청 (상품 상세 -> 채팅하기 버튼 클릭 시)
    @PostMapping("/chat/room")
    public String createRoom(@RequestParam Long itemId, HttpSession session) {
        Long buyerId = (Long) session.getAttribute("userId");
        if (buyerId == null) return "redirect:/";

        // 1. 채팅방 생성 (혹은 조회)
        Long roomId = chatService.createChatRoom(itemId, buyerId);

        // 2. 판매자 정보 알아내기 (알림 보내기 위해)
        ChatRoom room = chatService.findRoomById(roomId);
        Long sellerId = room.getSeller().getUserId();
        String buyerNickname = (String) session.getAttribute("nickname");

        // 3. ⭐ 판매자에게 실시간 알림 전송 ⭐
        // 만약 구매자가 본인이면(테스트용) 알림 안 보냄
        if (!sellerId.equals(buyerId)) {
            NotificationDto notification = NotificationDto.builder()
                    .message(buyerNickname + "님이 채팅을 시작했습니다.")
                    .senderNickname(buyerNickname)
                    .roomId(roomId)
                    .build();

            // "/sub/user/{sellerId}/noti" 채널로 쏜다!
            messagingTemplate.convertAndSend("/sub/user/" + sellerId + "/noti", notification);
        }

        return "redirect:/chat/room/" + roomId;
    }

    // 3. 메시지 전송 (WebSocket)
    // 클라이언트가 /pub/chat/message 로 보내면 여기서 잡음
// 3. 메시지 전송 (WebSocket)
    // 클라이언트가 /pub/chat/message 로 보내면 여기서 잡음
    @MessageMapping("/chat/message")
    public void message(ChatMessageDto message) {
        // 1. DB 저장 및 저장된 메시지 객체 반환 (기존 로직)
        ChatMessageDto savedMessage = chatService.saveMessage(message);

        // 2. 현재 채팅방에 있는 사람들(구독자)에게 메시지 전송 (기존 로직)
        // 화면: 채팅방 안에서 말풍선이 올라옴
        messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), savedMessage);

        // ▼▼▼ [추가] 3. 상대방에게 '실시간 알림' 보내기 ▼▼▼
        // 화면: 다른 페이지에 있어도 "새 메시지가 도착했습니다" 알림 뜸

        // (1) 채팅방 정보 조회 (수신자를 찾기 위해)
        ChatRoom room = chatService.findRoomById(message.getRoomId());

        // (2) 수신자(Receiver) 결정
        Long senderId = message.getSenderId();
        Long receiverId;

        // 보낸 사람이 판매자면 -> 받는 사람은 구매자
        if (room.getSeller().getUserId().equals(senderId)) {
            receiverId = room.getBuyer().getUserId();
        } else {
            // 보낸 사람이 구매자면 -> 받는 사람은 판매자
            receiverId = room.getSeller().getUserId();
        }

        // (3) 알림 메시지 생성 (예: "홍길동: 안녕하세요!")
        NotificationDto notification = NotificationDto.builder()
                .roomId(message.getRoomId())
                .senderNickname(savedMessage.getSenderNickname()) // 보낸 사람 닉네임
                .message(savedMessage.getMessage()) // 실제 메시지 내용
                .build();

        // (4) 상대방의 개인 알림 채널로 전송
        messagingTemplate.convertAndSend("/sub/user/" + receiverId + "/noti", notification);
    }

    // 채팅 목록 페이지 이동
    @GetMapping("/chat/list")
    public String myChatList(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/"; // 로그인 안 했으면 메인으로 쫓아냄
        }

        // 서비스에서 내 채팅방 목록 가져오기
        List<ChatRoomListDto> rooms = chatService.findAllRoom(userId);

        model.addAttribute("rooms", rooms);
        model.addAttribute("myUserId", userId); // 화면에서 '상대방 이름' 찾기 위해 필요

        return "chat/list";
    }
}