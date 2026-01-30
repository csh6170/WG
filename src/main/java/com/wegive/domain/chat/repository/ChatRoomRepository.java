package com.wegive.domain.chat.repository;

import com.wegive.domain.chat.entity.ChatRoom;
import com.wegive.domain.item.entity.Item;
import com.wegive.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    // 이미 만들어진 채팅방이 있는지 확인 (물건 + 구매자 기준)
    Optional<ChatRoom> findByItemAndBuyer(Item item, User buyer);

    // 내 채팅방 목록 조회 (내가 판매자이거나 구매자인 경우)
    @Query("SELECT r FROM ChatRoom r " +
            "JOIN FETCH r.item " +
            "JOIN FETCH r.seller " +
            "JOIN FETCH r.buyer " +
            "WHERE r.seller = :seller OR r.buyer = :buyer " +
            "ORDER BY r.roomId DESC")
    List<ChatRoom> findBySellerOrBuyerOrderByRoomIdDesc(User seller, User buyer);

    // [추가] 특정 상품에 연결된 모든 채팅방 찾기 (회원탈퇴 시 삭제용)
    List<ChatRoom> findByItem(Item item);

    // [추가] 상품 ID와 구매자 ID로 채팅방 찾기
    // ChatRoom 엔티티 내부의 item(Item)의 itemId와, buyer(User)의 userId를 참조합니다.
    Optional<ChatRoom> findByItem_ItemIdAndBuyer_UserId(Long itemId, Long userId);
}