package com.wegive.domain.admin.controller;

import com.wegive.domain.report.entity.Report;
import com.wegive.domain.report.service.ReportService;
import com.wegive.domain.user.entity.User;
import com.wegive.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/admin") // 모든 주소가 /admin 으로 시작함
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;

    /**
     * [관리자 메인 대시보드]
     * 주소: /admin
     */
    @GetMapping("")
    public String adminHome(HttpSession session, Model model) {
        // 1. 로그인/권한 체크 (기본 코드 유지)
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/";
        User user = userRepository.findById(userId).orElseThrow();
        if (!"ADMIN".equals(user.getRole())) return "redirect:/";

        // 2. 통계 데이터 가져오기
        long totalUsers = userRepository.count();
        long totalItems = itemRepository.count();

        LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        long todayItems = itemRepository.countByCreatedAtAfter(startOfToday);

        // 📍 [추가] 미처리 신고 건수 가져오기
        // reportService에 미처리 건수를 세는 메서드(예: countPendingReports)가 있다고 가정합니다.
        long pendingReports = reportService.countPendingReports();

        // 3. 모델에 담기
        model.addAttribute("nickname", user.getNickname());
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("todayItems", todayItems);
        model.addAttribute("pendingReports", pendingReports); // 📍 추가

        return "admin/adminpage";
    }
    /**
     * [회원 관리] 전체 회원 목록 조회
     */
    @GetMapping("/users")
    public String userList(Model model) {
        // 모든 회원 가져오기 (가입일 최신순)
        // 실무에선 페이징(Pageable)이 필수지만, 일단 리스트로 갑니다!
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("users", users);

        return "admin/adminpageusers";
    }

    /**
     * [회원 관리] 회원 정지/해제 (AJAX 요청)
     */
    @PostMapping("/users/{userId}/status")
    @ResponseBody // 화면이 아니라 데이터(문자열)만 반환
    public ResponseEntity<String> changeUserStatus(@PathVariable Long userId, @RequestParam String status) {
        User user = userRepository.findById(userId).orElseThrow();

        // 관리자(나)는 정지시킬 수 없음 (안전장치)
        if ("ADMIN".equals(user.getRole())) {
            return ResponseEntity.badRequest().body("관리자는 정지할 수 없습니다.");
        }

        user.changeStatus(status); // 상태 변경 (ACTIVE <-> BANNED)
        userRepository.save(user); // 저장

        return ResponseEntity.ok("상태가 변경되었습니다.");
    }
    // (추가) Service가 필요하므로 필드에 추가 (이미 있으면 패스)
    private final com.wegive.domain.item.service.ItemService itemService;
    private final com.wegive.domain.item.repository.ItemRepository itemRepository;

    /**
     * [상품 관리] 전체 상품 목록 조회
     */
    @GetMapping("/items")
    public String itemList(Model model) {
        // 모든 상품 가져오기 (최신순)
        List<com.wegive.domain.item.entity.Item> items = itemRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("items", items);
        return "admin/adminpageitems";
    }

    /**
     * [상품 관리] 강제 삭제 (숨김 처리)
     */
    @PostMapping("/items/{itemId}/delete")
    @ResponseBody
    public ResponseEntity<String> forceDeleteItem(@PathVariable Long itemId) {
        itemService.forceDeleteItem(itemId); // 관리자 권한으로 강제 삭제
        return ResponseEntity.ok("삭제(숨김) 처리되었습니다.");
    }
    /*------------------------------------------
    * 신고
    * ------------------------------------------*/
    private final ReportService reportService;
    /**
     * [신고 관리] 전체 신고 목록 조회
     * 사유: 관리자가 들어온 신고를 확인하는 페이지
     */
    @GetMapping("/reports")
    public String reportList(Model model) {
        List<Report> reports = reportService.getAllReports();
        model.addAttribute("reports", reports);
        return "admin/adminpagereports"; // 뷰 페이지
    }

    /**
     * [신고 관리] 신고 승인 및 유저 정지 (핵심 기능)
     * 사유: 신고된 내용이 맞다고 판단되면 해당 유저를 BANNED 시키고 신고를 처리 완료함.
     */
    @PostMapping("/reports/{reportId}/ban")
    @ResponseBody
    public ResponseEntity<String> banUserViaReport(@PathVariable Long reportId) {
        // 1. 신고 내역 가져오기
        Report report = reportService.getReport(reportId);

        // 2. 신고 당한 사람(B) 가져오기
        User targetUser = report.getReported();

        // 3. 유저 상태 'BANNED'로 변경 (User 엔티티의 편의 메서드 활용)
        // (기존 AdminController의 changeUserStatus 메서드와 유사한 로직)
        if ("ADMIN".equals(targetUser.getRole())) {
            return ResponseEntity.badRequest().body("관리자는 정지할 수 없습니다.");
        }

        targetUser.changeStatus("BANNED");
        userRepository.save(targetUser);

        // [수정] 결과("BANNED")와 함께 완료 처리
        reportService.completeReport(reportId, "BANNED");

        return ResponseEntity.ok("해당 유저가 정지되었고, 신고가 처리되었습니다.");
    }
    // [추가] 신고 반려 (증거 불충분 등)
    @PostMapping("/reports/{reportId}/reject")
    @ResponseBody
    public ResponseEntity<String> rejectReport(@PathVariable Long reportId) {
        // 유저는 정지하지 않고, 신고만 처리 완료로 변경
        reportService.completeReport(reportId, "REJECTED");
        return ResponseEntity.ok("신고가 반려 처리되었습니다.");
    }
}