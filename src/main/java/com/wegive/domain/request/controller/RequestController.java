package com.wegive.domain.request.controller;

import com.wegive.domain.request.dto.RequestCreateDto;
import com.wegive.domain.request.dto.RequestResponseDto;
import com.wegive.domain.request.service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * [Controller] 나눔 신청 관련 API
 * URL: http://localhost:8080/api/requests
 */
@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    // 1. 나눔 신청하기 (POST)
    @PostMapping
    public ResponseEntity<String> createRequest(
            @RequestParam Long userId, // (임시) 로그인 구현 전
            @RequestBody RequestCreateDto dto) {

        requestService.createRequest(userId, dto);
        return ResponseEntity.ok("나눔 신청이 완료되었습니다.");
    }

    // 2. [마이페이지] 내가 보낸 신청 목록 확인 (GET)
    @GetMapping("/me")
    public ResponseEntity<List<RequestResponseDto>> getMyRequests(@RequestParam Long userId) {
        return ResponseEntity.ok(requestService.getMyRequests(userId));
    }

    // 3. [나눔이용] 내 물건의 신청자 목록 확인 (GET)
    // URL 예: /api/requests/item/5 (5번 상품의 신청자들 보여줘)
    @GetMapping("/item/{itemId}")
    public ResponseEntity<List<RequestResponseDto>> getItemRequests(
            @RequestParam Long userId,
            @PathVariable Long itemId) {

        return ResponseEntity.ok(requestService.getRequestsByItem(userId, itemId));
    }

    // 4. [나눔이용] 당첨자 선정 (수락) (POST)
    // URL 예: /api/requests/10/accept (10번 신청자를 당첨시킨다)
    @PostMapping("/{reqId}/accept")
    public ResponseEntity<String> acceptRequest(
            @RequestParam Long userId,
            @PathVariable Long reqId) {

        requestService.acceptRequest(userId, reqId);
        return ResponseEntity.ok("당첨자가 선정되었습니다.");
    }
}