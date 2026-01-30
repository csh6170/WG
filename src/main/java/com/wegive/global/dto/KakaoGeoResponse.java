package com.wegive.global.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * [DTO] 카카오 로컬 API 응답 데이터를 받아내기 위한 그릇
 * 목적: JSON 형태의 복잡한 응답을 자바 객체로 변환 (Unmarshalling)
 * 사용: KakaoAddressService에서 RestTemplate 결과값으로 사용
 */
@Getter
@NoArgsConstructor
public class KakaoGeoResponse {

    private Meta meta;               // 응답 메타 데이터 (총 개수 등)
    private List<Document> documents; // 실제 주소 정보 리스트 (보통 1개가 옴)

    @Getter
    @NoArgsConstructor
    public static class Meta {
        @JsonProperty("total_count") // JSON의 "total_count" 키를 자바 변수 totalCount에 매핑
        private Integer totalCount;
    }

    @Getter
    @NoArgsConstructor
    public static class Document {
        @JsonProperty("address")      // 지번 주소 정보
        private Address address;

        @JsonProperty("road_address") // 도로명 주소 정보
        private RoadAddress roadAddress;
    }

    @Getter
    @NoArgsConstructor
    public static class Address {
        @JsonProperty("address_name")
        private String addressName;

        @JsonProperty("region_1depth_name")
        private String region1DepthName; // 시/도

        @JsonProperty("region_2depth_name")
        private String region2DepthName; // 구/군

        @JsonProperty("region_3depth_name")
        private String region3DepthName; // 동/읍/면 -> [핵심] 우리가 DB에 저장할 '동네 이름'
    }

    @Getter
    @NoArgsConstructor
    public static class RoadAddress {
        @JsonProperty("address_name")
        private String addressName;
    }
}