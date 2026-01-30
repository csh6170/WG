package com.wegive.domain.item.dto;

import com.wegive.domain.item.entity.Item;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [DTO] 상품 정보 응답 데이터
 * 역할: DB의 Entity(원본)를 그대로 내보내지 않고, 화면에 필요한 정보만 골라서 전달
 */
@Getter
@Builder
@Setter
public class ItemResponseDto {

    private Long itemId;            // 상품 ID
    private String title;           // 제목
    private String category;        // 카테고리
    private String description;     // 내용
    private String status;          // 나눔 상태 (AVAILABLE 등)

    private Integer viewCount;      // 조회수
    private Integer stock;          // 재고
    private String sellerNickname;  // 나눔이 닉네임 (ID 대신 닉네임 표시)
    private String sellerProfileImage; //  [추가] 나눔이 프로필 사진 필드
    private String addressName;     // 거래 장소
    // [수정] 지도 표시를 위해 Entity에 있는 좌표값을 가져옵니다.
    private Double itemLat;         // 위도
    private Double itemLon;         // 경도
    private LocalDateTime createdAt;// 작성일
    private List<String> imageUrls; // 이미지 URL 목록 (목록 조회 시엔 대표 이미지만, 상세 조회 시엔 전체)
    private Long sellerId;          // [추가] 나눔이 고유 번호 (이게 있어야 비교 가능!)
    private boolean liked;          // [추가] 내가 찜했는지 여부 (true: ❤️, false: 🤍)
    private int likeCount;          // [추가] 찜 개수 담을 변수
    private boolean isReviewed;     // [추가] 후기 작성 여부 (true/false)
    private Long buyerId;           // [추가] 나눔 받는 사람
    private Double sellerMannerTemp;// [추가] 판매자 매너온도 필드


    // [변환 메서드] Item Entity -> ItemResponseDto
    public static ItemResponseDto of(Item item, List<String> imageUrls) {
        return ItemResponseDto.builder()
                .itemId(item.getItemId())
                .title(item.getTitle())
                .category(item.getCategory())
                .description(item.getDescription())
                .status(item.getStatus())
                .viewCount(item.getViewCount())
                .stock(item.getStock())
                .sellerNickname(item.getSeller().getNickname()) // User Entity에서 닉네임 꺼내기
                .sellerProfileImage(item.getSeller().getProfileImg())   // [추가] 엔티티에서 사진 정보 꺼내서 담기
                .sellerId(item.getSeller().getUserId()) // [추가] 엔티티에서 ID 꺼내오기
                .addressName(item.getAddressName())
                .itemLat(item.getItemLat())
                .itemLon(item.getItemLon()) // [수정] Entity의 좌표값을 DTO에 넣어줍니다.
                .createdAt(item.getCreatedAt())
                .imageUrls(imageUrls) // 별도로 조회한 이미지 리스트 주입
                .liked(false) // 기본값은 false (Service에서 로그인 여부 체크 후 변경됨)
                .likeCount(0) // 기본값 0 (Service에서 채워줄 예정)
                .isReviewed("Y".equals(item.getIsReviewed())) // [추가] Y면 true
                .buyerId(item.getBuyer() != null ? item.getBuyer().getUserId() : null)  // [추가] 구매자 ID 넣기 (없으면 null)
                .sellerMannerTemp(item.getSeller().getMannerTemp()) // [추가] 엔티티에서 꺼내서 담기
                .build();
    }
}