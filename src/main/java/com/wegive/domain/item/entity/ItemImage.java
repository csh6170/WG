package com.wegive.domain.item.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * [Entity] ITEM_IMAGES 테이블 매핑
 * 역할: 상품에 첨부된 이미지 정보
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ITEM_IMAGES")
public class ItemImage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_img_gen")
    @SequenceGenerator(name = "seq_img_gen", sequenceName = "SEQ_ITEM_IMAGES", allocationSize = 1)
    @Column(name = "IMG_ID")
    private Long imgId;

    // 어떤 상품의 사진인지 연결 (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ITEM_ID", nullable = false)
    private Item item;

    @Column(name = "ORIGINAL_NAME", length = 200)
    private String originalName; // 사용자가 올린 파일명

    @Column(name = "STORED_NAME", length = 200)
    private String storedName;   // 서버에 저장된 파일명 (중복 방지용)

    @Column(name = "IS_THUMBNAIL", length = 1)
    private String isThumbnail;  // 'Y' or 'N' (대표 사진 여부)

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ItemImage(Item item, String originalName, String storedName, String isThumbnail) {
        this.item = item;
        this.originalName = originalName;
        this.storedName = storedName;
        this.isThumbnail = isThumbnail;
    }
}