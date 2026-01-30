package com.wegive.domain.item.repository;

import com.wegive.domain.item.entity.Item;
import com.wegive.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph; // 추가됨
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    // [최적화] 판매자(seller) 정보도 같이 가져옴 (JOIN FETCH)
    @EntityGraph(attributePaths = {"seller"})
    List<Item> findAllByStatusNotOrderByCreatedAtDesc(String status);

    // [최적화] 메인 화면 목록용
    @EntityGraph(attributePaths = {"seller"})
    List<Item> findAllByStatusNotInOrderByCreatedAtDesc(Collection<String> statuses);

    @EntityGraph(attributePaths = {"seller"})
    List<Item> findByCategoryAndStatusNotOrderByCreatedAtDesc(String category, String status);

    @EntityGraph(attributePaths = {"seller"})
    List<Item> findByTitleContainingOrDescriptionContainingOrderByCreatedAtDesc(String title, String description);

    // 마이페이지는 보통 데이터가 적으므로 필수는 아니지만 해두면 좋음
    @EntityGraph(attributePaths = {"seller"})
    List<Item> findBySellerOrderByCreatedAtDesc(User seller);

    @EntityGraph(attributePaths = {"seller"})
    List<Item> findByBuyerOrderByCreatedAtDesc(User buyer);

    // [관리자용] 전체 조회
    @EntityGraph(attributePaths = {"seller"})
    List<Item> findAllByOrderByCreatedAtDesc();

    List<Item> findByBuyerAndStatusOrderByCreatedAtDesc(User buyer, String status);

    List<Item> findByBuyer(User buyer);

    // [최적화] 검색 쿼리
    @EntityGraph(attributePaths = {"seller"})
    @Query("SELECT i FROM Item i WHERE i.status NOT IN ('HIDDEN', 'DELETE') " +
            "AND (:category IS NULL OR :category = '' OR i.category = :category) " +
            "AND (:keyword IS NULL OR :keyword = '' OR (i.title LIKE CONCAT('%', :keyword, '%') OR i.description LIKE CONCAT('%', :keyword, '%')))")
    Slice<Item> searchItems(@Param("category") String category,
                            @Param("keyword") String keyword,
                            Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime startOfDay);
}