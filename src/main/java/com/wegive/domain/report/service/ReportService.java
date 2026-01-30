package com.wegive.domain.report.service;

import com.wegive.domain.report.dto.ReportCreateRequestDto;
import com.wegive.domain.report.entity.Report;
import com.wegive.domain.report.repository.ReportRepository;
import com.wegive.domain.user.entity.User;
import com.wegive.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    /**
     * 기능: 신고 접수
     */
    public void createReport(Long reporterId, ReportCreateRequestDto dto) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("신고 하는 사람 정보 없음"));

        User reported = userRepository.findById(dto.getReportedUserId())
                .orElseThrow(() -> new RuntimeException("신고 당하는 사람 정보 없음"));

        Report report = Report.builder()
                .reporter(reporter)
                .reported(reported)
                .itemId(dto.getItemId())
                .reason(dto.getReason())
                .description(dto.getDescription())
                .status("PENDING") // 접수 대기 상태
                .build();

        reportRepository.save(report);
    }
    /**
     * 기능: 모든 신고 내역 조회 (관리자용)
     * 사유: AdminController에서 목록을 보여주기 위해 필요
     */
    @Transactional(readOnly = true)
    public List<Report> getAllReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 기능: 신고 단건 조회
     * 사유: 신고 처리 시 해당 엔티티를 찾기 위해 필요
     */
    @Transactional(readOnly = true)
    public Report getReport(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신고입니다."));
    }

    /**
     * 기능: 신고 상태 변경 (PENDING -> PROCESSED)
     * 사유: 유저 정지 후 신고 처리 완료 표시를 위해 필요
     */
    // [수정] 신고 처리 완료 (결과 포함 + 매너온도 하락 로직 추가)
    public void completeReport(Long reportId, String result) {
        Report report = getReport(reportId);

        // 1. 신고 상태 업데이트 (ENTITY)
        report.complete(result);

        // 2. [추가] 정지(BANNED)된 경우, 신고 당한 사람 매너온도 -1.0 깎기
        if ("BANNED".equals(result)) {
            User reportedUser = report.getReported(); // 신고 당한 사람
            if (reportedUser != null) {
                reportedUser.changeMannerTemp(-1.0); // 패널티 적용
            }
        }
    }

    // [추가] 내 신고 결과 알림 조회 (조회 후 바로 '읽음' 처리)
    public List<String> getMyReportNotifications(Long userId) {
        // 1. 알림 대상 조회
        List<Report> processedReports = reportRepository.findByReporter_UserIdAndStatusAndIsNotifiedFalse(userId, "PROCESSED");

        List<String> messages = new ArrayList<>();

        // 2. 메시지 생성 및 읽음 처리
        for (Report report : processedReports) {
            String targetNickname = report.getReported().getNickname();
            String msg = "";

            if ("BANNED".equals(report.getProcessResult())) {
                msg = String.format(" 신고하신 유저 '%s'님이 관리자에 의해 이용 정지되었습니다.", targetNickname);
            } else if ("REJECTED".equals(report.getProcessResult())) {
                msg = String.format(" 신고하신 유저 '%s'님은 증거 불충분으로 반려되었습니다.", targetNickname);
            }

            if (!msg.isEmpty()) {
                messages.add(msg);
            }

            report.markAsNotified(); // 읽음 처리 (더 이상 안 뜸)
        }

        return messages;
    }
    /**
     * 기능: 미처리 신고 건수 조회 (관리자용)
     * 사유: AdminController 대시보드에서 통계를 보여주기 위해 필요
     */
    @Transactional(readOnly = true)
    public long countPendingReports() {
        // 상태(status)가 "PENDING"인 신고 데이터의 개수를 카운트하여 반환합니다.
        // 해당 쿼리 메서드(countByStatus)가 ReportRepository에 정의되어 있어야 합니다.
        return reportRepository.countByStatus("PENDING");
    }
}