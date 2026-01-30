package com.wegive.domain.wish.repository;

import com.wegive.domain.wish.entity.Wishlist;
import com.wegive.domain.user.entity.User;
import com.wegive.domain.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * [Repository] 찜 목록 조회
 */
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    // 상세페이지 진입 시: 내가 찜했는지 확인
    boolean existsByUserAndItem(User user, Item item);

    // 찜 취소 시 사용: 데이터 찾기
    Optional<Wishlist> findByUserAndItem(User user, Item item);

    // [마이페이지] 나의 찜 목록
    List<Wishlist> findByUserOrderByCreatedAtDesc(User user);

    // [추가] 이 상품을 몇 명이 찜했는지 숫자 세기
    int countByItem(Item item);

    // [추가] 특정 사용자의 찜 싹 지우기
    void deleteByUser(User user);
    // [추가] 특정 물건에 달린 찜 싹 지우기
    void deleteByItem(Item item);

    // [추가] 특정 물건을 찜한 모든 내역 가져오기 (나눔 대상자 후보)
    List<Wishlist> findByItem(Item item);
}