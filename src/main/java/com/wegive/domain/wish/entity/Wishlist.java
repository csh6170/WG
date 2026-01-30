package com.wegive.domain.wish.entity;

import com.wegive.domain.item.entity.Item;
import com.wegive.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * [Entity] WISHLISTS 테이블 매핑 (찜하기)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "WISHLISTS", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"USER_ID", "ITEM_ID"}) // 중복 찜 방지
})
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_wish_gen")
    @SequenceGenerator(name = "seq_wish_gen", sequenceName = "SEQ_WISHLISTS", allocationSize = 1)
    @Column(name = "WISH_ID")
    private Long wishId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ITEM_ID", nullable = false)
    private Item item;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    // --- [추가 코드] ---
    @Builder
    public Wishlist(User user, Item item) {
        this.user = user;
        this.item = item;
    }
}