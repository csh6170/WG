package com.wegive.domain.login.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NaverLoginService {

    @Value("${oauth.naver.client-id}")
    private String clientId;

    @Value("${oauth.naver.client-secret}")
    private String clientSecret;

    @Value("${oauth.naver.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate;

    /**
     * 기능: 인가 코드(Code)를 받아서 사용자 정보(프로필)를 반환
     */
    public Map<String, Object> getUserInfo(String code, String state) throws JsonProcessingException {
        // 1. 인가 코드로 엑세스 토큰 요청
        String accessToken = getAccessToken(code, state);

        // 2. 엑세스 토큰으로 사용자 정보(프로필) 요청
        return getNaverProfile(accessToken);
    }

    // 1. 토큰 발급 요청
    private String getAccessToken(String code, String state) throws JsonProcessingException {
        String reqUrl = "https://nid.naver.com/oauth2.0/token";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("state", state); // 네이버는 state 값이 필수 (보안용)

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(reqUrl, HttpMethod.POST, request, String.class);

        // JSON 파싱 (accessToken 추출)
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        return jsonNode.get("access_token").asText();
    }

    // 2. 프로필 정보 요청
    private Map<String, Object> getNaverProfile(String accessToken) throws JsonProcessingException {
        String reqUrl = "https://openapi.naver.com/v1/nid/me";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(reqUrl, HttpMethod.POST, request, String.class);

        // JSON 파싱
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(response.getBody());

        // 네이버는 "response"라는 키 안에 실제 정보가 들어있음
        JsonNode responseNode = jsonNode.get("response");

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", responseNode.get("id").asText());
        userInfo.put("nickname", responseNode.get("nickname").asText());
        userInfo.put("email", responseNode.get("email").asText());

        // [수정] 프로필 이미지 기본값 처리 로직 추가
        String profileImage = "/images/default_profile.png"; // 1. 기본값 설정

//        if (responseNode.has("profile_image")) {
//            String naverImg = responseNode.get("profile_image").asText();
//            // 2. 네이버 프사가 유효하면 덮어쓰기
//            if (naverImg != null && !naverImg.isEmpty()) {
//                profileImage = naverImg;
//            }
//        }

        // [중요] 최종 결정된 이미지를 맵에 담기 (이 줄이 꼭 있어야 합니다!)
        userInfo.put("profile_image", profileImage);

        return userInfo;
    }
}