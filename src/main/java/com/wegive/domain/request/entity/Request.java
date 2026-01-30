package com.wegive.domain.request.entity;

import com.wegive.domain.item.entity.Item;
import com.wegive.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * [Entity] REQUESTS 테이블 매핑 (나눔 신청)
 * 제약: 한 유저가 같은 상품 중복 신청 불가 (UniqueConstraint)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "REQUESTS", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ITEM_ID", "BUYER_ID"})
})
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_req_gen")
    @SequenceGenerator(name = "seq_req_gen", sequenceName = "SEQ_REQUESTS", allocationSize = 1)
    @Column(name = "REQ_ID")
    private Long reqId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ITEM_ID", nullable = false)
    private Item item; // 신청한 상품

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BUYER_ID", nullable = false)
    private User buyer; // 신청자

    @Column(name = "REQ_STATUS", length = 20)
    private String reqStatus; // WAITING, ACCEPTED

    @CreationTimestamp
    @Column(name = "REQ_TIME", updatable = false)
    private LocalDateTime reqTime; // 선착순 판별 기준 시간

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    // --- [Service 사용을 위한 추가 코드] ---

    @Builder
    public Request(Item item, User buyer, String reqStatus) {
        this.item = item;
        this.buyer = buyer;
        this.reqStatus = reqStatus;
    }

    // 상태 변경 메서드 (수락/거절용)
    public void changeStatus(String status) {
        this.reqStatus = status;
    }
}