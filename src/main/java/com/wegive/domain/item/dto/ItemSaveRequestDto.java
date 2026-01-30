package com.wegive.domain.item.dto;

import com.wegive.domain.item.entity.Item;
import com.wegive.domain.user.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ItemSaveRequestDto {

    private String title;           // 제목
    private String description;     // 내용 (CLOB)
    private String category;        // 카테고리
    private Double itemLat;         // 거래 장소 위도
    private Double itemLon;         // 거래 장소 경도
    private String addressName;     // 거래 장소 이름 (예: 판교역)

    // [중요] 이미지 파일은 MultipartFile 타입으로 받습니다.
    private List<MultipartFile> imageFiles = new ArrayList<>();

    // DTO -> Entity 변환 메서드 (이미지는 별도로 처리하므로 여기선 제외)
    public Item toEntity(User seller) {
        return Item.builder()
                .title(title)
                .description(description)
                .category(category)
                .itemLat(itemLat)
                .itemLon(itemLon)
                .addressName(addressName)
                .seller(seller)        // 나눔이 정보 등록
                .stock(1)              // 기본 재고 1
                .status("ON_SALE")     // 기본 나눔 중 상태
                .viewCount(0)
                .build();
    }
}