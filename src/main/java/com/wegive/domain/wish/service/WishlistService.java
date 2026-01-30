package com.wegive.domain.wish.service;

import com.wegive.domain.item.entity.Item;
import com.wegive.domain.item.repository.ItemRepository;
import com.wegive.domain.user.entity.User;
import com.wegive.domain.user.repository.UserRepository;
import com.wegive.domain.wish.dto.WishlistResponseDto;
import com.wegive.domain.wish.entity.Wishlist;
import com.wegive.domain.wish.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    /**
     * 기능: 찜하기 토글 (Toggle)
     * 로직: 이미 찜했으면 취소(삭제), 아니면 찜(저장)
     * 반환: true(찜 성공), false(찜 취소)
     */
    public boolean toggleWishlist(Long userId, Long itemId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("회원 없음"));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("상품 없음"));

        // 1. 이미 찜한 상태인지 확인
        Optional<Wishlist> existingWish = wishlistRepository.findByUserAndItem(user, item);

        if (existingWish.isPresent()) {
            // 2-A. 이미 있으면 -> 삭제 (찜 취소)
            wishlistRepository.delete(existingWish.get());
            return false; // 하트 꺼짐
        } else {
            // 2-B. 없으면 -> 저장 (찜 하기)
            Wishlist wishlist = Wishlist.builder()
                    .user(user)
                    .item(item)
                    .build();
            wishlistRepository.save(wishlist);
            return true; // 하트 켜짐
        }
    }
    /**
     * 기능: 내가 찜한 목록 보기
     */
    @Transactional(readOnly = true)
    public List<WishlistResponseDto> getMyWishlist(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("회원 없음"));

        return wishlistRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(WishlistResponseDto::from)
                .collect(Collectors.toList());
    }
    /**
     * [추가] 상품의 총 찜 개수 조회
     * ItemService에서 사용하던 wishlistRepository.countByItem(item)을 여기서도 사용합니다.
     */
    @Transactional(readOnly = true)
    public int countWishes(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("상품 없음"));

        // 이미 Repository에 구현되어 있는 메서드 호출
        return wishlistRepository.countByItem(item);
    }
}