package com.wegive.domain.item.entity;

import com.wegive.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ITEMS")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_items_gen")
    @SequenceGenerator(name = "seq_items_gen", sequenceName = "SEQ_ITEMS", allocationSize = 1)
    @Column(name = "ITEM_ID")
    private Long itemId;

    // [최적화 1] 판매자 정보 지연 로딩 (Repository에서 Fetch Join으로 한 번에 가져올 예정)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SELLER_ID", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BUYER_ID")
    private User buyer;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    @Column(name = "CATEGORY", length = 50)
    private String category;

    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "ITEM_LAT", nullable = false, columnDefinition = "NUMBER(10, 7)")
    private Double itemLat;

    @Column(name = "ITEM_LON", nullable = false, columnDefinition = "NUMBER(10, 7)")
    private Double itemLon;

    @Column(name = "ADDRESS_NAME", nullable = false, length = 100)
    private String addressName;

    // ▼▼▼ [수정] 중복된 리스트 삭제하고 하나로 통합 & 최적화 ▼▼▼
    // [최적화 2] @BatchSize: 이미지를 100개씩 묶어서 가져옴 (N+1 해결)
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ItemImage> itemImages = new ArrayList<>();

    @Column(name = "STATUS", length = 20)
    private String status;

    @ColumnDefault("1")
    @Column(name = "STOCK")
    private Integer stock;

    @ColumnDefault("0")
    @Column(name = "VIEW_COUNT")
    private Integer viewCount;

    // ▼▼▼ [최적화 3] 찜 개수 자동 계산 (가상 컬럼) ▼▼▼
    // 별도의 SELECT COUNT(*) 쿼리를 날리지 않고, 상품 조회 시 서브쿼리로 한 번에 가져옴
    @Formula("(SELECT count(w.wish_id) FROM wishlists w WHERE w.item_id = item_id)")
    private int likeCount;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "IS_REVIEWED", columnDefinition = "VARCHAR(1) DEFAULT 'N'")
    private String isReviewed = "N";

    // --- 비즈니스 메서드 ---

    @Builder
    public Item(User seller, String title, String category, String description,
                Double itemLat, Double itemLon, String addressName,
                String status, Integer stock, Integer viewCount) {
        this.seller = seller;
        this.title = title;
        this.category = category;
        this.description = description;
        this.itemLat = itemLat;
        this.itemLon = itemLon;
        this.addressName = addressName;
        this.status = status;
        this.stock = stock;
        this.viewCount = viewCount;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void confirmReview() {
        this.isReviewed = "Y";
    }

    public void removeBuyer() {
        this.buyer = null;
    }

    // [편의 메서드] 이미지 리스트 getter 통일 (DTO 등에서 images나 itemImages 둘 다 쓸 수 있게)
    public List<ItemImage> getImages() {
        return itemImages;
    }
}