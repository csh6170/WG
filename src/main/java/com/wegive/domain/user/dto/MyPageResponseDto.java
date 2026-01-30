package com.wegive.domain.user.dto;

import com.wegive.domain.item.dto.ItemResponseDto;
import com.wegive.domain.user.entity.User;
import com.wegive.domain.wish.dto.WishlistResponseDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MyPageResponseDto {
    // 1. 내 정보
    private Long userId;
    private String nickname;
    private Double mannerTemp;
    private String profileImg;
    private String addressName; // [추가] 동네 이름 (예: 역삼동)

    // 2. 리스트 데이터
    private List<ItemResponseDto> mySharingItems;  // 1. 나눔 활동 (내가 준 거)
    private List<ItemResponseDto> myReceivedItems; // 2. [추가] 나눔 받은 내역 (내가 받은 거)
    private List<WishlistResponseDto> myWishes;    // 3. 관심 목록 (내가 찜한 거)

    public static MyPageResponseDto of(User user,
                                       List<ItemResponseDto> mySharingItems,
                                       List<ItemResponseDto> myReceivedItems, // 파라미터 추가
                                       List<WishlistResponseDto> myWishes) {
        return MyPageResponseDto.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .mannerTemp(user.getMannerTemp())
                .profileImg(user.getProfileImg())
                .addressName(user.getAddressName()) // [추가] DB에서 꺼내서 담기
                .mySharingItems(mySharingItems)
                .myReceivedItems(myReceivedItems) // 빌더 추가
                .myWishes(myWishes)
                .build();
    }
}