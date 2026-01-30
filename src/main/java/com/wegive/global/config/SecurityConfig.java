package com.wegive.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 해제 (개발 편의성을 위해 일단 끔, 실무에선 켜는 게 좋음)
                .csrf(csrf -> csrf.disable())

                // 2. 권한 설정 (순서 중요! 좁은 범위 -> 넓은 범위 순서로)
                .authorizeHttpRequests(auth -> auth
                        // [1. 정적 자원 & 에러] 누구나 접근 가능
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/error").permitAll()

                        // [2. 관리자 전용]
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // [3. 인증(로그인)이 필요한 기능들 - 여기가 대폭 추가됨!]
                        .requestMatchers(
                                "/mypage/**",           // 마이페이지
                                "/items/new",           // 상품 등록 화면
                                "/items/*/edit",        // 상품 수정 화면
                                "/chat/**",             // [추가] 채팅 관련 전체
                                "/api/wishes/**",       // 찜 기능
                                "/api/reports/**",      // 신고 기능
                                "/api/requests/**",     // [추가] 나눔 신청 기능
                                "/api/users/**"         // [추가] 회원 정보 수정/탈퇴/위치인증
                        ).authenticated()

                        // [4. 상품 API 세부 설정 (GET은 허용, 나머지는 인증 필요)]
                        // 중요: 상품 목록(GET)은 누구나 보지만, 등록(POST)은 로그인해야 함
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/items").authenticated() // 등록
                        .requestMatchers("/api/items/*/delete", "/api/items/*/edit", "/api/items/*/status", "/api/items/*/wishers", "/api/items/*/review").authenticated() // 수정/삭제/상태변경 등

                        // [5. 나머지] 메인화면, 상품 상세(GET), 로그인 콜백 등은 누구나 접근 가능
                        .anyRequest().permitAll()
                )

                // 3. 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/logout") // 이 주소로 요청 오면 로그아웃 처리
                        .logoutSuccessUrl("/") // 성공 시 메인으로
                        .invalidateHttpSession(true) // 세션 날리기
                        .deleteCookies("JSESSIONID") // 쿠키 삭제
                )

                // 4. 예외 처리 (로그인 안 한 사람이 회원 페이지 접근 시)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            // 로그인 페이지로 바로 보내지 않고, 메인으로 보내면서 파라미터 전달
                            // 프론트(home.html)에서 이걸 보고 로그인 모달을 띄울 예정!
                            response.sendRedirect("/?error=login_required");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            // 권한 없는 페이지(예: 일반 유저가 관리자 페이지 접속) 접근 시
                            response.sendRedirect("/?error=access_denied");
                        })
                );

        return http.build();
    }
}