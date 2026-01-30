package com.wegive.global.service;

import com.wegive.global.dto.KakaoGeoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * [Service] 카카오 지도 API와 직접 통신하는 역할
 * 역할: 좌표(숫자)를 주면 -> 행정동(글자)으로 바꿔주는 통역사
 */
@Service
@RequiredArgsConstructor
public class KakaoAddressService {

    private final RestTemplate restTemplate; // HTTP 요청 도구 (Config에서 설정함)

    @Value("${kakao.rest-api-key}") // application.yml에 있는 키 값을 가져옴
    private String kakaoRestApiKey;

    // 카카오 로컬 API URL (좌표 -> 주소 변환)
    private static final String KAKAO_API_URL = "https://dapi.kakao.com/v2/local/geo/coord2address.json";

    /**
     * 기능: 위도/경도를 받아 행정동 이름을 반환
     * 호출: UserService.updateProfile() 에서 호출됨
     */
    public String getDongName(Double lat, Double lon) {
        // 1. 헤더 설정 (카카오가 요구하는 인증 방식: "KakaoAK {KEY}")
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 2. 요청 URL 조립 (GET 파라미터)
        String url = KAKAO_API_URL + "?x=" + lon + "&y=" + lat;

        try {
            // 3. 카카오 서버로 GET 요청 전송 및 응답 수신
            ResponseEntity<KakaoGeoResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, KakaoGeoResponse.class
            );

            // 4. 응답 데이터 검증 (데이터가 비어있으면 실패 처리)
            if (response.getBody() == null || response.getBody().getDocuments().isEmpty()) {
                return "주소 미확인";
            }

            // 5. 가장 중요한 '동 이름(region_3depth_name)'만 쏙 뽑아서 반환
            return response.getBody().getDocuments().get(0).getAddress().getRegion3DepthName();

        } catch (Exception e) {
            e.printStackTrace(); // 에러 로그 출력
            return "위치 확인 실패"; // 카카오 서버 오류 시 기본값 반환
        }
    }
}