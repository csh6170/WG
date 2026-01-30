package com.wegive.domain.report.entity;

import com.wegive.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "REPORTS")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_rpt_gen")
    @SequenceGenerator(name = "seq_rpt_gen", sequenceName = "SEQ_REPORTS", allocationSize = 1)
    @Column(name = "REPORT_ID")
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REPORTER_ID", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REPORTED_ID", nullable = false)
    private User reported;

    @Column(name = "ITEM_ID")
    private Long itemId;

    @Column(name = "REASON", nullable = false, length = 50)
    private String reason;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "STATUS", length = 20)
    private String status;

    @CreationTimestamp
    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "PROCESS_RESULT")
    private String processResult; // 처리 결과 ("BANNED", "REJECTED")

    @Column(name = "IS_NOTIFIED")
    private Boolean isNotified = false; // 신고자에게 알림을 보냈는지 여부 (기본값 false)

    @Builder
    public Report(User reporter, User reported, Long itemId, String reason, String description, String status) {
        this.reporter = reporter;
        this.reported = reported;
        this.itemId = itemId;
        this.reason = reason;
        this.description = description;
        this.status = status;
    }

    // ▼▼▼ [추가된 코드] ▼▼▼
    // 사유: 관리자가 신고를 처리 완료했을 때 상태를 변경하기 위함
    public void updateStatus(String status) {
        this.status = status;
    }

    // [수정] 결과까지 받아서 상태 변경
    public void complete(String result) {
        this.status = "PROCESSED";
        this.processResult = result;
        this.isNotified = false; // 처리는 됐지만 아직 알림은 안 본 상태
    }

    // [추가] 알림 확인 처리
    public void markAsNotified() {
        this.isNotified = true;
    }
}