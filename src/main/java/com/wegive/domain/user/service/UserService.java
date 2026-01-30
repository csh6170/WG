package com.wegive.domain.user.service;

import com.wegive.domain.chat.entity.ChatRoom;
import com.wegive.domain.chat.repository.ChatMessageRepository;
import com.wegive.domain.chat.repository.ChatRoomRepository;
import com.wegive.domain.item.dto.ItemResponseDto;
import com.wegive.domain.item.entity.Item;
import com.wegive.domain.item.entity.ItemImage;
import com.wegive.domain.item.repository.ItemImageRepository;
import com.wegive.domain.item.repository.ItemRepository;
import com.wegive.domain.report.repository.ReportRepository;
import com.wegive.domain.request.repository.RequestRepository;
import com.wegive.domain.user.dto.MyPageResponseDto;
import com.wegive.domain.user.dto.UserUpdateRequestDto;
import com.wegive.domain.user.entity.User;
import com.wegive.domain.user.repository.UserRepository;
import com.wegive.domain.wish.dto.WishlistResponseDto;
import com.wegive.domain.wish.repository.WishlistRepository;
import com.wegive.global.service.KakaoAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map; // 👈 [필수] 이게 없으면 Map에서 빨간 줄 뜹니다!
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * [Service] 회원 정보 관련 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final KakaoAddressService kakaoAddressService;
    // [추가] 마이페이지 데이터를 조회하기 위해 필요한 저장소들
    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;
    private final WishlistRepository wishlistRepository;
    // [추가] 채팅방 삭제를 위해 주입
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RequestRepository requestRepository;
    private final ReportRepository reportRepository;
    // [추가] 파일 저장 경로 (application.yml에서 가져옴)
    @Value("${file.dir}")
    private String fileDir;

    /**
     * 기능: 프로필 이미지 변경 (새로 추가됨)
     */
    public void updateProfileImage(Long userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        if (file.isEmpty()) return;

        // 1. 파일명 생성 (UUID)
        String originalFilename = file.getOriginalFilename();
        String storeFileName = UUID.randomUUID() + "_" + originalFilename;

        // 2. 파일 저장
        file.transferTo(new File(fileDir + storeFileName));

        // 3. DB 업데이트 (이미지 경로 변경)
        // 화면에서 불러올 때 /images/파일명 으로 불러오도록 경로 설정
        user.updateSocialInfo(user.getNickname(), "/images/" + storeFileName);
    }

    /**
     * 기능: 프로필 수정 및 동네 인증 (기존 코드)
     * 흐름: 1. 유저 찾기 -> 2. 카카오 API로 좌표를 동네 이름으로 변환 -> 3. DB 업데이트
     */
    public void updateProfile(Long userId, UserUpdateRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("회원 없음"));

        // 카카오 API 호출 (좌표 -> "삼평동")
        String dongName = kakaoAddressService.getDongName(dto.getMyLat(), dto.getMyLon());

        // Entity 내부 메서드를 통해 정보 변경 (Dirty Checking으로 자동 저장)
        user.updateProfile(dto.getNickname(), dto.getMyLat(), dto.getMyLon(), dongName);
    }

    /**
     * [통합 소셜 로그인]
     * 수정: 신규 가입 시 닉네임을 'User_' + DB PK(userId)로 설정
     */
    public Long socialLogin(String provider, Map<String, Object> userInfo) {
        String email = (String) userInfo.get("email");
        // 닉네임은 아래에서 생성하므로 여기서 읽을 필요 없음
        String providerId = (String) userInfo.get("id");
        String profileImage = (String) userInfo.get("profile_image");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // [1단계] 임시 닉네임으로 객체 생성
            // 이유: DB에 넣기 전에는 userId가 없음. 하지만 nickname은 NOT NULL이라 비워둘 수 없음.
            // 따라서 잠시 UUID로 채워둡니다.
            String tempNickname = "TEMP_" + UUID.randomUUID().toString().substring(0, 8);

            user = User.builder()
                    .email(email)
                    .nickname(tempNickname) // 임시 값
                    .provider(provider)
                    .providerId(providerId)
                    .profileImg(profileImage)
                    .role("USER")
                    .userStatus("ACTIVE")
                    .mannerTemp(36.5)
                    .build();

            // [2단계] 저장 (이 순간 DB 시퀀스가 작동하여 userId가 생성됨)
            userRepository.save(user);

            // [3단계] 생성된 userId를 가져와서 닉네임 업데이트
            // 예: User_1, User_105 등
            String finalNickname = "User_" + user.getUserId();

            // 엔티티 업데이트 (Dirty Checking으로 인해 트랜잭션 종료 시점에 DB에 반영됨)
            user.updateSocialInfo(finalNickname, profileImage);
        }
        // else: 기존 회원은 정보 유지 (아무것도 안 함)

        return user.getUserId();
    }
    // [추가] 마이페이지 데이터 조회 (새로 추가된 메서드)
    @Transactional(readOnly = true)
    public MyPageResponseDto getMyPageData(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        // (1) 나눔 활동 (Seller가 나)
        List<Item> sellItems = itemRepository.findBySellerOrderByCreatedAtDesc(user);
        List<ItemResponseDto> mySharingItems = convertToDtoList(sellItems);

        // (2) 나눔 받은 내역 (Buyer가 나 + 완료된 것)
        List<Item> buyItems = itemRepository.findByBuyerAndStatusOrderByCreatedAtDesc(user, "COMPLETED");
        List<ItemResponseDto> myReceivedItems = convertToDtoList(buyItems);

        // (3) 관심 목록
        List<WishlistResponseDto> myWishes = wishlistRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(WishlistResponseDto::from)
                .collect(Collectors.toList());

        return MyPageResponseDto.of(user, mySharingItems, myReceivedItems, myWishes);
    }

    // [헬퍼 메서드] N+1 문제 완벽 해결 버전
    private List<ItemResponseDto> convertToDtoList(List<Item> items) {
        return items.stream().map(item -> {
            // 1. [최적화] DB 조회 대신, 메모리에 로딩된 이미지 리스트에서 필터링
            // (@BatchSize 덕분에 여기서 쿼리가 최적화되어 나갑니다)
            String thumbnailUrl = item.getImages().stream()
                    .filter(img -> "Y".equals(img.getIsThumbnail()))
                    .map(ItemImage::getStoredName)
                    .findFirst()
                    .orElse(null);

            List<String> urls = thumbnailUrl != null ? List.of(thumbnailUrl) : Collections.emptyList();

            ItemResponseDto dto = ItemResponseDto.of(item, urls);

            // 2. [최적화] DB 조회 대신, @Formula로 미리 계산된 값 사용
            // (추가 쿼리 발생 X)
            dto.setLikeCount(item.getLikeCount());

            return dto;
        }).collect(Collectors.toList());
    }

    // 2. [추가] 닉네임 변경
    public void updateNickname(Long userId, String newNickname) {
        // 1. 중복 검사
        if (userRepository.existsByNickname(newNickname)) {
            throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
        }

        // 2. 변경 진행
        User user = userRepository.findById(userId).orElseThrow();
        user.updateSocialInfo(newNickname, user.getProfileImg());
    }

    // 3. 회원 탈퇴 (수정된 버전)
    /**
     * 회원 탈퇴 (수정됨)
     */
    @Transactional
    public void withdrawUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        // [1] 신고 내역 삭제 (신고자 혹은 대상자일 경우) 🔴 핵심 추가
        // 제약 조건 에러의 주범일 확률이 높습니다.
        reportRepository.deleteByReporter(user);
        reportRepository.deleteByReported(user);

        // [2] 내가 누른 '찜' 모두 삭제
        wishlistRepository.deleteByUser(user);

        // [3] 내가 올린 '나눔 물품' 및 관련 데이터 정리
        List<Item> myItems = itemRepository.findBySellerOrderByCreatedAtDesc(user);
        for (Item item : myItems) {
            wishlistRepository.deleteByItem(item);
            itemImageRepository.deleteByItem(item);

            // 이 물건에 달린 채팅방과 메시지 싹 제거
            List<ChatRoom> itemRooms = chatRoomRepository.findByItem(item);
            for (ChatRoom room : itemRooms) {
                chatMessageRepository.deleteByChatRoom(room);
                chatRoomRepository.delete(room);
            }

            requestRepository.deleteByItem(item);
            itemRepository.delete(item);
        }

        // [4] 내가 '구매자(참여자)'로서 활동한 채팅방 정리
        // 판매글을 지울 때 안 지워진 '남의 글에 참여한 채팅방'을 지웁니다.
        List<ChatRoom> myParticipatedRooms = chatRoomRepository.findBySellerOrBuyerOrderByRoomIdDesc(user, user);
        for (ChatRoom room : myParticipatedRooms) {
            if (chatRoomRepository.existsById(room.getRoomId())) {
                chatMessageRepository.deleteByChatRoom(room);
                chatRoomRepository.delete(room);
            }
        }

        // [5] 내가 '구매자'로서 신청했던 내역 및 받은 물건 처리
        List<Item> receivedItems = itemRepository.findByBuyer(user);
        for (Item item : receivedItems) {
            item.removeBuyer(); // 아이템에서 구매자 정보만 NULL로 변경
        }
        requestRepository.deleteByBuyer(user); // 내 신청 기록 삭제

        // [6] 최종적으로 사용자 삭제
        userRepository.delete(user);
    }

    // [추가] 내 동네(위치) 인증/변경 기능
    public String updateLocation(Long userId, Double lat, Double lon) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        // 1. 카카오 API로 좌표 -> 행정동 이름 변환 (기존 서비스 재사용)
        String dongName = kakaoAddressService.getDongName(lat, lon);

        // 2. 닉네임은 그대로 두고, 위치 정보만 업데이트
        // (updateProfile 메서드를 재사용하거나, 위치만 바꾸는 메서드를 엔티티에 만들어도 됨)
        user.updateProfile(user.getNickname(), lat, lon, dongName);

        return dongName; // 변경된 동네 이름 반환
    }

    // [추가] 매너온도 올리기 (후기 보내기)
    public void sendThanks(Long buyerId, Long itemId) {
        // 1. 상품 조회
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("상품 없음"));

        // 2. 검증 (구매자가 맞는지, 이미 후기를 썼는지)
        if (item.getBuyer() == null || !item.getBuyer().getUserId().equals(buyerId)) {
            throw new IllegalArgumentException("구매자만 매너온도를 올릴 수 있습니다.");
        }
        if ("Y".equals(item.getIsReviewed())) {
            throw new IllegalStateException("이미 매너온도가 올라갔습니다.");
        }

        // 3. 나눔이 찾기
        User seller = userRepository.findById(item.getSeller().getUserId())
                .orElseThrow(() -> new IllegalArgumentException("나눔이 없음"));

        // 4. [수정] 엔티티 메서드 호출로 변경 (코드가 훨씬 깔끔해짐)
        // 기존: 복잡한 if문 로직 -> 변경: 한 줄로 끝
        seller.changeMannerTemp(0.5);

        // 5. 상품에 '후기 작성됨' 표시
        item.confirmReview();
    }
    /**
     * [추가] 유저 단건 조회 (로그인 시 닉네임 가져오기용)
     */
    @Transactional(readOnly = true)
    public User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
    }
}