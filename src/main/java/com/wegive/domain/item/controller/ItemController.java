package com.wegive.domain.item.controller;

import com.wegive.domain.chat.dto.ChatMessageDto;
import com.wegive.domain.chat.repository.ChatRoomRepository;
import com.wegive.domain.item.dto.ItemResponseDto;
import com.wegive.domain.item.dto.ItemSaveRequestDto;
import com.wegive.domain.item.service.ItemService;
import com.wegive.domain.wish.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * [Controller] 상품 관련 API 및 화면 연결
 */
@RestController
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final WishlistService wishlistService; // 찜하기 로직을 위해 필요할 수 있음 (또는 ItemService에 통합)
    private final SimpMessagingTemplate messagingTemplate; // 👈 추가
    private final ChatRoomRepository chatRoomRepository;   // 👈 추가 (방 번호 찾기용)

    // ==========================================
    //  1. 화면 연결 (HTML 보여주기) - 주소: /items/...
    // ==========================================

    /**
     * [GET] 상품 등록 페이지 이동
     * 주소: /items/new (화면)
     */
    @GetMapping("/items/new") // 👈 (수정) 전체 주소 명시
    public ModelAndView showItemForm() {
        return new ModelAndView("item-form");
    }

    /**
     * [GET] 상품 상세 페이지 이동
     * 주소: /items/{itemId} (화면)
     */
    @GetMapping("/items/{itemId}") // 👈 (수정) 전체 주소 명시
    public ModelAndView showItemDetail(@PathVariable Long itemId) {
        ModelAndView mv = new ModelAndView("item-detail");
        mv.addObject("itemId", itemId);
        return mv;
    }

    // ==========================================
    //  2. API (데이터 처리) - 주소: /api/items/...
    // ==========================================

    /**
     * [POST] 상품 등록 (파일 업로드 포함)
     * 주소: /api/items
     */
    /**
     * [POST] 상품 등록
     */
    @PostMapping("/api/items")
    public ResponseEntity<String> saveItem(
            @ModelAttribute ItemSaveRequestDto requestDto,
            HttpSession session) throws IOException {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("로그인 필요");

        // [핵심] 파일 개수 체크
        if (requestDto.getImageFiles() != null && requestDto.getImageFiles().size() > 5) {
            return ResponseEntity.badRequest().body("이미지는 최대 5장까지만 업로드 가능합니다.");
        }

        itemService.saveItem(userId, requestDto);
        return ResponseEntity.ok("상품 등록 완료!");
    }

    /**
     * [GET] 전체 상품 목록 조회
     * 주소: /api/items
     */
/*    @GetMapping("/api/items") // 👈 (수정) /api 붙여줌
    public ResponseEntity<List<ItemResponseDto>> getAllItems() {
        return ResponseEntity.ok(itemService.getAllItems());
    }*/

    /**
     * [GET] 상품 상세 정보 조회 (JSON 데이터)
     * 주소: /api/items/{itemId}
     */
    @GetMapping("/api/items/{itemId}") // 👈 (수정) /api 붙여줌
    public ResponseEntity<ItemResponseDto> getItemDetail(
            @PathVariable Long itemId,
            @RequestParam(required = false) Long userId) { // 👈 [수정] 파라미터 추가!
        // [디버깅용] 콘솔에 출력해보기
        System.out.println("상품 조회 요청 - itemId: " + itemId + ", userId: " + userId);
        // [수정] 서비스에 userId와 itemId를 둘 다 전달
        return ResponseEntity.ok(itemService.getItemDetail(userId, itemId));
    }

    /**
     * [변경] 상품 삭제 (POST -> DELETE)
     * 주소: DELETE /api/items/{itemId}
     */
    @DeleteMapping("/api/items/{itemId}") // 👈 POST에서 DELETE로 변경
    public ResponseEntity<String> deleteItem(
            @PathVariable Long itemId,
            @SessionAttribute(name = "userId", required = false) Long userId
    ) {
        if (userId == null) return ResponseEntity.status(401).body("로그인 필요");
        try {
            itemService.deleteItem(userId, itemId);
            return ResponseEntity.ok("삭제 완료");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("삭제 중 오류 발생");
        }
    }

    //  수정 기능 (Update)

    /**
     * [GET] 수정 페이지 이동 (기존 데이터 채워서 보냄)
     */
    @GetMapping("/items/{itemId}/edit")
    public ModelAndView showEditForm(@PathVariable Long itemId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        // [수정] 서비스 메서드가 (userId, itemId)를 원하므로 userId를 같이 넘겨줍니다.
        // 로그인을 안 했으면 userId가 null일 텐데, 서비스에서도 null 체크를 하므로 괜찮습니다.
        ItemResponseDto item = itemService.getItemDetail(userId, itemId);

        // 남의 글 수정하려 하면 튕겨내기
        if (userId == null || !item.getSellerId().equals(userId)) {
            return new ModelAndView("redirect:/"); // 메인으로 쫓아냄
        }

        ModelAndView mv = new ModelAndView("item-edit"); // item-edit.html로 이동
        mv.addObject("item", item);
        return mv;
    }

    /**
     * [POST] 상품 수정 요청
     */
    @PostMapping("/api/items/{itemId}/edit")
    public ResponseEntity<String> updateItem(
            @PathVariable Long itemId,
            @ModelAttribute ItemSaveRequestDto requestDto,
            HttpSession session) throws IOException {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("로그인 필요");

        // [핵심] 수정 시에도 파일 개수 체크
        if (requestDto.getImageFiles() != null && requestDto.getImageFiles().size() > 5) {
            return ResponseEntity.badRequest().body("새로 올리는 이미지는 최대 5장까지 가능합니다.");
        }

        try {
            itemService.updateItem(userId, itemId, requestDto);
            return ResponseEntity.ok("수정 완료");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }
    /**
     * [PATCH] 상품 상태 변경
     * 요청: /api/items/{itemId}/status?status=RESERVED
     */
    @PatchMapping("/api/items/{itemId}/status")
    public ResponseEntity<String> updateItemStatus(
            @PathVariable Long itemId,
            @RequestParam String status,
            @RequestParam(required = false) Long buyerId,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        try {
            // 1. 기존 상태 변경 로직 수행
            itemService.updateStatus(userId, itemId, status, buyerId);

            // 2. 🔴 [추가] 나눔 완료(COMPLETED) 시 채팅방에 종료 신호 전송
            if ("COMPLETED".equals(status) && buyerId != null) {
                // 상품과 구매자 정보를 통해 채팅방 ID를 조회합니다.
                chatRoomRepository.findByItem_ItemIdAndBuyer_UserId(itemId, buyerId)
                        .ifPresent(room -> {
                            ChatMessageDto statusMsg = ChatMessageDto.builder()
                                    .roomId(room.getRoomId())
                                    .senderId(0L) // 시스템 메시지용 ID
                                    .message("STATUS_CHANGED_TO_COMPLETED")
                                    .build();

                            // 해당 채팅방을 구독 중인 모든 유저에게 신호 전송
                            messagingTemplate.convertAndSend("/sub/chat/room/" + room.getRoomId(), statusMsg);
                        });
            }

            return ResponseEntity.ok("상태가 변경되었습니다.");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }
    /**
     * [추가] 찜하기 토글 API
     * 주소: POST /api/items/{itemId}/like
     */
    @PostMapping("/api/items/{itemId}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable Long itemId,
            @RequestParam Long userId) {

        // 1. 찜 토글
        boolean isLiked = wishlistService.toggleWishlist(userId, itemId);

        // 2. 찜 개수 조회
        int likeCount = wishlistService.countWishes(itemId);

        // 3. 결과 반환
        return ResponseEntity.ok(Map.of(
                "isLiked", isLiked,
                "likeCount", likeCount
        ));
    }
    // [추가] 찜한 이웃 목록 조회 API (팝업용)
    @GetMapping("/api/items/{itemId}/chat-partners")
    public ResponseEntity<List<Map<String, Object>>> getItemChatPartners(@PathVariable Long itemId) {
        return ResponseEntity.ok(itemService.getChatPartners(itemId));
    }
    /**
     * [GET] 전체 상품 목록 조회 (검색 기능, 페이징 추가)
     * 요청: /api/items?category=ELECTRONICS&keyword=자전거
     */
    @GetMapping("/api/items")
    public ResponseEntity<Slice<ItemResponseDto>> getAllItems(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,  // 👈 페이지 번호 (0부터 시작)
            @RequestParam(defaultValue = "12") int size  // 👈 한 번에 가져올 개수
    ) {
        // 최신순 정렬(createdAt Desc) 적용
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return ResponseEntity.ok(itemService.getAllItems(category, keyword, pageable));
    }
}