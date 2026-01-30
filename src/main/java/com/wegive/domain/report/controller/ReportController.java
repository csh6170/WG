package com.wegive.domain.report.controller;

import com.wegive.domain.report.dto.ReportCreateRequestDto;
import com.wegive.domain.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // 신고하기 (POST)
    @PostMapping
    public ResponseEntity<String> createReport(
            @RequestParam Long userId, // 신고자
            @RequestBody ReportCreateRequestDto dto) {

        reportService.createReport(userId, dto);
        return ResponseEntity.ok("신고가 정상적으로 접수되었습니다.");
    }
    // [추가] 내 신고 결과 알림 확인 (로그인 시 호출)
    @GetMapping("/notifications")
    public ResponseEntity<List<String>> checkNotifications(@RequestParam Long userId) {
        List<String> notifications = reportService.getMyReportNotifications(userId);
        return ResponseEntity.ok(notifications);
    }
}