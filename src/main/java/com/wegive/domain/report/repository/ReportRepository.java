package com.wegive.domain.report.repository;

import com.wegive.domain.report.entity.Report;
import com.wegive.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * [Repository] 신고 내역 조회
 */
public interface ReportRepository extends JpaRepository<Report, Long> {
    // 최신순 조회
    List<Report> findAllByOrderByCreatedAtDesc();
    // [추가] 알림용 조회: (신고자ID + 상태=PROCESSED + 알림여부=false)
    List<Report> findByReporter_UserIdAndStatusAndIsNotifiedFalse(Long reporterId, String status);
    // [추가] 상태별 신고 건수를 카운트하는 메서드
    long countByStatus(String status);

    void deleteByReporter(User reporter); // 내가 신고한 내역 삭제
    void deleteByReported(User reported); // 내가 신고당한 내역 삭제
}