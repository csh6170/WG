package com.wegive.global.config;

import com.wegive.global.interceptor.LoginCheckInterceptor; // 인터셉터 import
import lombok.RequiredArgsConstructor; // 추가
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry; // 추가
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor // 생성자 주입을 위해 추가 (중요!)
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.dir}")
    private String fileDir;

    // 만든 인터셉터를 가져옴
    private final LoginCheckInterceptor loginCheckInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // "http://localhost:8080/images/..." 로 요청이 오면
        // 실제로는 "file:///G:/wegive_uploads/" 폴더를 찾아가라!
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:///" + fileDir);
    }
    // [추가된 코드] 인터셉터 등록
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginCheckInterceptor)
                .addPathPatterns("/**") // 모든 경로 검사
                .excludePathPatterns(   // 검사 제외할 경로
                        "/", "/login/**", "/api/**", "/ws-stomp/**", // 웹소켓 등 예외 추가
                        "/css/**", "/js/**", "/images/**", "/error", "/favicon.ico",
                        "/admin/**" // (선택) 관리자 페이지는 관리자 필터가 따로 있다면 제외 가능
                );
    }
}