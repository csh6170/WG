package com.wegive.domain.item.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * [DTO] 상품 등록 요청 데이터
 * 역할: 사용자가 입력한 제목, 내용, 위치, 이미지 등을 담는 그릇
 */
@Getter
@NoArgsConstructor
public class ItemCreateRequestDto {

    private String title;           // 글 제목
    private String category;        // 카테고리 (전자기기, 가구 등)
    private String description;     // 상세 내용

    // 거래 희망 장소 (사용자의 현재 위치와 다를 수 있음)
    private Double itemLat;
    private Double itemLon;
    private String addressName;     // 예: 판교역 1번출구

    // 첨부 이미지 URL 리스트 (예: ["img1.jpg", "img2.jpg"])
    private List<String> imageUrls;
}