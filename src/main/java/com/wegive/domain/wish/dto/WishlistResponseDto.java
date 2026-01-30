package com.wegive.domain.wish.dto;

import com.wegive.domain.wish.entity.Wishlist;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class WishlistResponseDto {
    private Long wishId;
    private Long itemId;
    private String itemTitle;
    private String itemStatus;
    private LocalDateTime createdAt;

    // Entity -> DTO 변환
    public static WishlistResponseDto from(Wishlist wishlist) {
        return WishlistResponseDto.builder()
                .wishId(wishlist.getWishId())
                .itemId(wishlist.getItem().getItemId())
                .itemTitle(wishlist.getItem().getTitle())
                .itemStatus(wishlist.getItem().getStatus())
                .createdAt(wishlist.getCreatedAt())
                .build();
    }
}