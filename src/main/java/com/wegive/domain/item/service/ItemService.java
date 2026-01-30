package com.wegive.domain.item.service;

import com.wegive.domain.item.dto.ItemResponseDto;
import com.wegive.domain.item.dto.ItemSaveRequestDto;
import com.wegive.domain.item.entity.Item;
import com.wegive.domain.item.entity.ItemImage;
import com.wegive.domain.item.repository.ItemImageRepository;
import com.wegive.domain.item.repository.ItemRepository;
import com.wegive.domain.user.entity.User;
import com.wegive.domain.user.repository.UserRepository;
import com.wegive.domain.wish.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.wegive.domain.chat.repository.ChatRoomRepository; // 채팅방 조회용

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * [Service] 상품 관련 비즈니스 로직 처리
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;
    private final UserRepository userRepository;
    private final WishlistRepository wishlistRepository;
    private final ChatRoomRepository chatRoomRepository; // 채팅방 리포지토리 주입
    @Value("${file.dir}")
    private String fileDir;

    /**
     * 기능: 상품 등록
     */
    public Long saveItem(Long userId, ItemSaveRequestDto dto) throws IOException {
        User seller = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        Item item = Item.builder()
                .seller(seller)
                .title(dto.getTitle())
                .category(dto.getCategory())
                .description(dto.getDescription())
                .itemLat(dto.getItemLat())
                .itemLon(dto.getItemLon())
                .addressName(dto.getAddressName())
                .status("AVAILABLE")
                .stock(1)
                .viewCount(0)
                .build();

        itemRepository.save(item);

        List<MultipartFile> files = dto.getImageFiles();

        // 이미지 저장 루프 시작
        if (files != null && !files.isEmpty()) {
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);

                // [디버깅] 현재 처리 중인 파일 번호와 이름 출력
                System.out.println("===== [File Upload Debug] =====");
                System.out.println((i + 1) + "번째 이미지 처리 중: " + file.getOriginalFilename());
                System.out.println("파일 크기: " + file.getSize() + " bytes");

                if (file.isEmpty()) {
                    System.out.println((i + 1) + "번째 파일이 비어있어 스킵합니다.");
                    continue;
                }

                // 파일명 생성 및 물리 저장
                String originalFilename = file.getOriginalFilename();
                String storeFileName = UUID.randomUUID() + "_" + originalFilename;
                String fullPath = fileDir + storeFileName;

                file.transferTo(new File(fullPath));
                System.out.println("저장 완료 경로: " + fullPath);

                // 0번째 인덱스만 썸네일(Y)로 설정
                String isThumb = (i == 0) ? "Y" : "N";

                ItemImage image = ItemImage.builder()
                        .item(item)
                        .originalName(originalFilename)
                        .storedName(storeFileName)
                        .isThumbnail(isThumb)
                        .build();

                itemImageRepository.save(image);
                System.out.println((i + 1) + "번째 이미지 DB 저장 성공");
            }
        }

        System.out.println("전체 상품 등록 프로세스 완료. 반환 ID: " + item.getItemId());
        return item.getItemId();
    }

    /**
     * [수정] 기능: 전체 상품 목록 조회 (최신순)
     * 변경사항: DTO 변환 시 likeCount(찜 개수)를 조회하여 주입하도록 수정
     */
    @Transactional(readOnly = true)
    public List<ItemResponseDto> getAllItems() {
        // 1. 제외할 상태 목록 정의 (숨김, 삭제)
        List<String> excludedStatuses = List.of("HIDDEN", "DELETE");

        // 2. 리포지토리 호출
        List<Item> items = itemRepository.findAllByStatusNotInOrderByCreatedAtDesc(excludedStatuses);

        // 3. Entity List -> DTO List 변환
        return items.stream().map(item -> {
            List<ItemImage> images = itemImageRepository.findByItemAndIsThumbnail(item, "Y");
            List<String> urls = images.stream()
                    .map(ItemImage::getStoredName)
                    .collect(Collectors.toList());

            ItemResponseDto dto = ItemResponseDto.of(item, urls);

            // ⭐ [핵심 수정] 목록 조회 시에도 찜 개수(likeCount)를 채워줍니다.
            dto.setLikeCount(wishlistRepository.countByItem(item));

            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * [추가] 기능: 마이페이지용 - 내가 나눔 중인 상품 목록 조회
     * 설명: 마이페이지 Controller에서 이 메서드를 호출하여 데이터를 가져오면 likeCount가 정상 출력됩니다.
     */
    @Transactional(readOnly = true)
    public List<ItemResponseDto> getMySharingItems(Long userId) {
        User seller = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        // 내가 쓴 글 조회 (ItemRepository에 정의된 메서드 사용)
        List<Item> items = itemRepository.findBySellerOrderByCreatedAtDesc(seller);

        return items.stream().map(item -> {
            // 이미지 가져오기
            List<ItemImage> images = itemImageRepository.findByItemAndIsThumbnail(item, "Y");
            List<String> urls = images.stream()
                    .map(ItemImage::getStoredName)
                    .collect(Collectors.toList());

            ItemResponseDto dto = ItemResponseDto.of(item, urls);

            // ⭐ [핵심] 여기서 찜 개수를 넣어줘야 마이페이지에 숫자가 뜹니다!
            dto.setLikeCount(wishlistRepository.countByItem(item));

            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 기능: 상품 상세 조회
     */
    public ItemResponseDto getItemDetail(Long userId, Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("상품 없음"));

        if (userId == null || !item.getSeller().getUserId().equals(userId)) {
            item.setViewCount(item.getViewCount() + 1);
        }

        List<ItemImage> images = itemImageRepository.findByItemOrderByImgIdAsc(item);
        List<String> urls = images.stream()
                .map(ItemImage::getStoredName)
                .collect(Collectors.toList());

        ItemResponseDto dto = ItemResponseDto.of(item, urls);

        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                boolean isLiked = wishlistRepository.existsByUserAndItem(user, item);
                dto.setLiked(isLiked);
            }
        }
        // 상세 조회는 이미 잘 되어 있습니다.
        int count = wishlistRepository.countByItem(item);
        dto.setLikeCount(count);

        return dto;
    }

    /**
     * [수정] 상품 삭제 (Soft Delete)
     * 변경사항: 반환타입을 void로 유지하되, Controller에서 DELETE 매핑으로 처리
     */
    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        // ... (기존 로직 동일: 권한 체크 -> 찜 삭제 -> 상태 변경)
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("상품 없음"));

        if (!item.getSeller().getUserId().equals(userId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }
        wishlistRepository.deleteAll(wishlistRepository.findByItem(item));
        item.setStatus("DELETE");
    }

    public void updateItem(Long userId, Long itemId, ItemSaveRequestDto dto) throws IOException {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("상품 없음"));

        if (!item.getSeller().getUserId().equals(userId)) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        item.setTitle(dto.getTitle());
        item.setCategory(dto.getCategory());
        item.setDescription(dto.getDescription());
        item.setAddressName(dto.getAddressName());
        item.setItemLat(dto.getItemLat());
        item.setItemLon(dto.getItemLon());

        List<MultipartFile> files = dto.getImageFiles();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                String originalFilename = file.getOriginalFilename();
                String storeFileName = UUID.randomUUID() + "_" + originalFilename;
                file.transferTo(new File(fileDir + storeFileName));

                ItemImage image = ItemImage.builder()
                        .item(item)
                        .originalName(originalFilename)
                        .storedName(storeFileName)
                        .isThumbnail("N")
                        .build();

                itemImageRepository.save(image);
            }
        }
    }

    public void updateStatus(Long userId, Long itemId, String status, Long buyerId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("상품 없음"));

        if (!item.getSeller().getUserId().equals(userId)) {
            throw new IllegalStateException("상태 변경 권한이 없습니다.");
        }

        item.setStatus(status);

        if ("COMPLETED".equals(status) && buyerId != null) {
            User buyer = userRepository.findById(buyerId)
                    .orElseThrow(() -> new IllegalArgumentException("대상자 없음"));
            item.setBuyer(buyer);
        } else if ("AVAILABLE".equals(status)) {
            item.setBuyer(null);
        }
    }

    /**
     * [추가/변경] 나눔 완료 대상 조회 (찜한 사람 -> 채팅한 사람)
     * 기존 getWishers 대신 사용합니다.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getChatPartners(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("상품 없음"));

        // 이 아이템으로 생성된 채팅방을 모두 찾아서, 상대방(구매희망자) 정보를 반환
        return chatRoomRepository.findByItem(item).stream()
                .map(chatRoom -> {
                    User buyer = chatRoom.getBuyer(); // 채팅방의 구매자
                    return Map.<String, Object>of(
                            "userId", buyer.getUserId(),
                            "nickname", buyer.getNickname()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 기능: 전체 상품 목록 조회 (검색 + 카테고리)
     * [수정] 검색 결과 목록에도 찜 개수(likeCount)가 나오도록 수정
     */
    @Transactional(readOnly = true)
    public Slice<ItemResponseDto> getAllItems(String category, String keyword, Pageable pageable) {
        Slice<Item> slice = itemRepository.searchItems(category, keyword, pageable);

        return slice.map(item -> {
            List<ItemImage> images = itemImageRepository.findByItemAndIsThumbnail(item, "Y");
            List<String> urls = images.isEmpty() ? List.of() : List.of(images.get(0).getStoredName());

            ItemResponseDto dto = ItemResponseDto.of(item, urls);

            // ⭐ [핵심 수정] 검색 결과에도 찜 개수 추가
            dto.setLikeCount(wishlistRepository.countByItem(item));

            return dto;
        });
    }

    public void forceDeleteItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("상품 없음"));

        item.setStatus("HIDDEN");
    }
}