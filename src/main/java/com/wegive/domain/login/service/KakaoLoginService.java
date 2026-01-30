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
public class KakaoLoginService {

    @Value("${kakao.rest-api-key}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate;

    public Map<String, Object> getUserInfo(String code) throws JsonProcessingException {
        String accessToken = getAccessToken(code);
        return getKakaoProfile(accessToken);
    }

    // 1. 인가 코드로 토큰 발급 요청
    private String getAccessToken(String code) throws JsonProcessingException {
        String reqUrl = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(reqUrl, HttpMethod.POST, request, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        return jsonNode.get("access_token").asText();
    }

    // 2. 토큰으로 사용자 정보 조회 (최종 수정: 이메일 강제 생성 버전)
    private Map<String, Object> getKakaoProfile(String accessToken) throws JsonProcessingException {
        String reqUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(reqUrl, HttpMethod.POST, request, String.class);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(response.getBody());

        long id = jsonNode.get("id").asLong(); // 회원번호
        String idStr = String.valueOf(id);

        JsonNode kakaoAccount = jsonNode.get("kakao_account");



        // [핵심] 이메일이 없으면 ID를 이용해서 임시 이메일 생성!
        // 예: 123456789@kakao.social
        String email = idStr + "@kakao.social";

        // 닉네임과 프로필 사진 기본값 설정
        String nickname = "익명 사용자";
        String profileImage = "https://github.com/user-attachments/assets/774a82da-80c5-40ca-af9e-35b1bb980ffb"; // 1. 기본 이미지로 시작

        if (kakaoAccount != null && kakaoAccount.has("profile")) {
            JsonNode profile = kakaoAccount.get("profile");

            // (1) 닉네임 가져오기 (이 부분이 있어야 합니다!)
            if (profile.has("nickname")) {
                nickname = profile.get("nickname").asText();
            }

            // (2) 프로필 사진 가져오기 (유효한 경우에만 덮어쓰기)
            if (profile.has("profile_image_url")) {
                String kakaoImg = profile.get("profile_image_url").asText();
                // null도 아니고 빈 문자열("")도 아닐 때만 적용
                if (kakaoImg != null && !kakaoImg.isEmpty()) {
                    profileImage = kakaoImg;
                }
            }
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", idStr);
        userInfo.put("nickname", nickname);
        userInfo.put("email", email); // 이제 절대 null이거나 빈 값이 들어가지 않음
        userInfo.put("profile_image", profileImage);

        return userInfo;
    }
}