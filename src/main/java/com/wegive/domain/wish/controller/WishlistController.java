package com.wegive.domain.wish.controller;

import com.wegive.domain.wish.dto.WishlistResponseDto;
import com.wegive.domain.wish.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishes")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    // 1. 찜하기 토글 (POST)
    // URL: /api/wishes/item/5 (5번 상품 찜/취소)
    @PostMapping("/item/{itemId}")
    public ResponseEntity<String> toggleWishlist(
            @RequestParam Long userId,
            @PathVariable Long itemId) {

        boolean isLiked = wishlistService.toggleWishlist(userId, itemId);
        return ResponseEntity.ok(isLiked ? "찜 완료" : "찜 취소");
    }

    // 2. 내 찜 목록 보기 (GET)
    @GetMapping("/me")
    public ResponseEntity<List<WishlistResponseDto>> getMyWishlist(@RequestParam Long userId) {
        return ResponseEntity.ok(wishlistService.getMyWishlist(userId));
    }
}