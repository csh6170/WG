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
public class GoogleLoginService {

    @Value("${oauth.google.client-id}")
    private String clientId;

    @Value("${oauth.google.client-secret}")
    private String clientSecret;

    @Value("${oauth.google.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate;

    /**
     * 기능: 구글 인가 코드로 사용자 정보 가져오기
     */
    public Map<String, Object> getUserInfo(String code) throws JsonProcessingException {
        String accessToken = getAccessToken(code);
        return getGoogleProfile(accessToken);
    }

    // 1. 토큰 발급
    private String getAccessToken(String code) throws JsonProcessingException {
        String reqUrl = "https://oauth2.googleapis.com/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri); // 구글 콘솔 설정과 100% 일치해야 함
        body.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(reqUrl, HttpMethod.POST, request, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        return jsonNode.get("access_token").asText();
    }

    // 2. 프로필 조회
    private Map<String, Object> getGoogleProfile(String accessToken) throws JsonProcessingException {
        // 구글 사용자 정보 요청 API
        String reqUrl = "https://www.googleapis.com/oauth2/v2/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(reqUrl, HttpMethod.GET, request, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(response.getBody());

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", jsonNode.get("id").asText()); // 구글의 고유 회원 ID
        userInfo.put("nickname", jsonNode.get("name").asText()); // 구글은 'name'이 닉네임
        userInfo.put("email", jsonNode.get("email").asText());

        // [수정] 프로필 이미지 기본값 처리 로직 추가
        String profileImage = "https://github.com/user-attachments/assets/774a82da-80c5-40ca-af9e-35b1bb980ffb"; // 1. 기본값 설정

//        if (jsonNode.has("picture")) {
//            String googleImg = jsonNode.get("picture").asText();
//            // 2. 구글 프사가 유효하면 덮어쓰기
//            if (googleImg != null && !googleImg.isEmpty()) {
//                profileImage = googleImg;
//            }
//        }

        // 3. 최종 결정된 이미지 경로 저장
        userInfo.put("profile_image", profileImage);

        return userInfo;
    }
}