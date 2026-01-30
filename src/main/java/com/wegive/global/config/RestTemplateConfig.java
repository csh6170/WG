package com.wegive.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // Builder 대신 직접 객체 생성
        RestTemplate restTemplate = new RestTemplate();

        // 타임아웃 설정을 위해 Factory 객체 생성
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5000ms = 5초
        factory.setReadTimeout(5000);    // 5000ms = 5초

        restTemplate.setRequestFactory(factory);

        return restTemplate;
    }
}