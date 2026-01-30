package com.wegive.domain.user.controller;

import com.wegive.domain.login.service.GoogleLoginService;
import com.wegive.domain.login.service.KakaoLoginService;
import com.wegive.domain.login.service.NaverLoginService;
import com.wegive.domain.user.dto.MyPageResponseDto;
import com.wegive.domain.user.dto.UserUpdateRequestDto;
import com.wegive.domain.user.entity.User;
import com.wegive.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final KakaoLoginService kakaoLoginService;   // 카카오
    private final NaverLoginService naverLoginService;   // 네이버
    private final GoogleLoginService googleLoginService; // 구글
    // [추가] 시큐리티 컨텍스트를 세션에 저장/복원하는 저장소
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    // ==========================================
    //  1. 화면 연결 (HTML 보여주기)
    // ==========================================

    // [추가] 마이페이지 화면 이동 (/mypage)
    @GetMapping("/mypage")
    public ModelAndView myPage(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return new ModelAndView("redirect:/"); // 로그인 안 했으면 메인으로
        }

        MyPageResponseDto myPageData = userService.getMyPageData(userId);

        ModelAndView mv = new ModelAndView("mypage");
        mv.addObject("data", myPageData);
        return mv;
    }

    // [추가] 프로필 이미지 변경 API
    @PostMapping("/api/users/{userId}/profileImage")
    public ResponseEntity<String> updateProfileImage(
            @PathVariable Long userId,
            @RequestParam("profileImage") MultipartFile file) throws IOException {

        userService.updateProfileImage(userId, file);
        return ResponseEntity.ok("프로필 이미지가 변경되었습니다.");
    }
    /**
     * [내 정보 수정 및 동네 인증]
     * 주소: /api/users/{userId}
     */
    @PutMapping("/api/users/{userId}") // 👈 여기에 전체 주소를 적어줍니다.
    public ResponseEntity<String> updateProfile(
            @PathVariable Long userId,
            @RequestBody UserUpdateRequestDto requestDto) {

        userService.updateProfile(userId, requestDto);
        return ResponseEntity.ok("정보가 수정되었습니다.");
    }

    // ==========================================
    //            소셜 로그인 콜백
    // ==========================================

    /**
     * 1. 카카오 로그인 콜백
     * 주소: http://localhost:8080/login/oauth2/code/kakao
     */
    // 1. 카카오
    @GetMapping("/login/oauth2/code/kakao")
    public void kakaoCallback(@RequestParam String code, HttpSession session,
                              HttpServletRequest request,   // 👈 request 추가
                              HttpServletResponse response) throws IOException {
        Map<String, Object> userInfo = kakaoLoginService.getUserInfo(code);

        // 1. 로그인/가입 처리 (DB에는 User_PK로 저장됨)
        Long userId = userService.socialLogin("kakao", userInfo);

        // 2. [수정] DB에서 최신 유저 정보 가져오기
        User user = userService.findUser(userId);
        // [추가] 정지된 유저(BANNED) 로그인 차단
        if ("BANNED".equals(user.getUserStatus())) {
            response.sendRedirect("/?error=banned");
            return;
        }

        // 3. [수정] 소셜 닉네임이 아닌, DB의 진짜 닉네임을 세션에 저장
        session.setAttribute("userId", userId);
        session.setAttribute("nickname", user.getNickname()); // 👈 DB 닉네임 사용

        // ▼▼▼ [수정] request, response도 같이 넘겨줍니다! ▼▼▼
        forceLogin(user, request, response);

        response.sendRedirect("/");
    }
    /**
     * 2. 네이버 로그인 콜백
     * 주소: http://localhost:8080/login/oauth2/code/naver
     */
    // 2. 네이버
    @GetMapping("/login/oauth2/code/naver")
    public void naverCallback(@RequestParam String code, @RequestParam String state, HttpSession session,
                              HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
        Map<String, Object> userInfo = naverLoginService.getUserInfo(code, state);
        Long userId = userService.socialLogin("naver", userInfo);

        // [수정]
        User user = userService.findUser(userId);
        // [추가] 정지된 유저(BANNED) 로그인 차단
        if ("BANNED".equals(user.getUserStatus())) {
            response.sendRedirect("/?error=banned"); // 메인으로 쫓아내기
            return; // 메서드 종료 (세션 생성 안 함)
        }

        session.setAttribute("userId", userId);
        session.setAttribute("nickname", user.getNickname()); // 👈 DB 닉네임 사용

        // ▼▼▼ [추가] 스프링 시큐리티에도 로그인 알리기! ▼▼▼
        forceLogin(user, request, response);

        response.sendRedirect("/");
    }
    /**
     * 3. 구글 로그인 콜백
     * 주소: http://localhost:8080/login/oauth2/code/google
     */
    // 3. 구글
    @GetMapping("/login/oauth2/code/google")
    public void googleCallback(@RequestParam String code, HttpSession session,
                               HttpServletRequest request,HttpServletResponse response) throws IOException {
        Map<String, Object> userInfo = googleLoginService.getUserInfo(code);
        Long userId = userService.socialLogin("google", userInfo);

        // [수정]
        User user = userService.findUser(userId);
        // [추가] 정지된 유저(BANNED) 로그인 차단
        if ("BANNED".equals(user.getUserStatus())) {
            response.sendRedirect("/?error=banned"); // 메인으로 쫓아내기
            return; // 메서드 종료 (세션 생성 안 함)
        }

        session.setAttribute("userId", userId);
        session.setAttribute("nickname", user.getNickname()); // 👈 DB 닉네임 사용

        // ▼▼▼ [추가] 스프링 시큐리티에도 로그인 알리기! ▼▼▼
        forceLogin(user, request, response);

        response.sendRedirect("/");
    }

    // 4. 로그아웃 (세션 삭제)
    @GetMapping("/logout")
    public void logout(HttpSession session, HttpServletResponse response) throws IOException {
        session.invalidate(); // 세션 전체 삭제 (로그아웃)
        response.sendRedirect("/");
    }

    // [수정] 닉네임 변경 API (중복 예외 처리 추가)
    @PatchMapping("/api/users/{userId}/nickname")
    public ResponseEntity<String> updateNickname(@PathVariable Long userId, @RequestParam String nickname, HttpSession session) {
        try {
            // 서비스 호출 (중복이면 여기서 에러 발생)
            userService.updateNickname(userId, nickname);

            // 세션 업데이트 (성공했을 때만)
            session.setAttribute("nickname", nickname);

            return ResponseEntity.ok("닉네임이 변경되었습니다.");

        } catch (IllegalStateException e) {
            // 중복된 경우 400 에러와 함께 메시지 반환
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // [추가] 회원 탈퇴 API
    @DeleteMapping("/api/users/{userId}")
    public ResponseEntity<String> withdrawUser(@PathVariable Long userId, HttpSession session) {
        userService.withdrawUser(userId);
        session.invalidate(); // 세션 삭제 (로그아웃)
        return ResponseEntity.ok("탈퇴가 완료되었습니다.");
    }

    // [추가] 위치 인증 API
    @PatchMapping("/api/users/{userId}/location")
    public ResponseEntity<String> updateLocation(
            @PathVariable Long userId,
            @RequestParam Double lat,
            @RequestParam Double lon) {

        String newDong = userService.updateLocation(userId, lat, lon);
        return ResponseEntity.ok(newDong + "(으)로 동네 인증이 완료되었습니다. 📍");
    }

    // [추가] 감사 인사(후기) 보내기 API
    @PostMapping("/api/items/{itemId}/review")
    public ResponseEntity<String> sendThanks(@PathVariable Long itemId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body("로그인 필요");

        try {
            userService.sendThanks(userId, itemId);
            return ResponseEntity.ok("나눔이의 매너온도가 올라갔습니다!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    /**
     * [Spring Security] 강제 로그인 처리 (Spring Boot 3.x / Security 6 대응 버전)
     */
    private void forceLogin(User user, HttpServletRequest request, HttpServletResponse response) {
        String role = user.getRole();
        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, List.of(authority));

        // 1. Context 생성 및 설정
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // 2. [핵심] 세션에 SecurityContext를 명시적으로 저장 (이게 없으면 로그인 풀림!)
        securityContextRepository.saveContext(context, request, response);
    }
}