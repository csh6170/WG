package com.wegive.global.interceptor;

import com.wegive.domain.user.entity.User;
import com.wegive.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class LoginCheckInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);

        // 1. 로그인된 사용자만 검사
        if (session != null && session.getAttribute("userId") != null) {
            Long userId = (Long) session.getAttribute("userId");

            // 2. DB에서 최신 상태 조회
            User user = userRepository.findById(userId).orElse(null);

            // 3. 정지(BANNED) 상태라면?
            if (user != null && "BANNED".equals(user.getUserStatus())) {
                session.invalidate(); // 강제 로그아웃
                response.sendRedirect("/?error=banned"); // 메인으로 추방
                return false; // 컨트롤러 진입 차단
            }
        }
        return true; // 통과
    }
}