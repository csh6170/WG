package com.wegive.domain.request.service;

import com.wegive.domain.item.entity.Item;
import com.wegive.domain.item.repository.ItemRepository;
import com.wegive.domain.request.dto.RequestCreateDto;
import com.wegive.domain.request.dto.RequestResponseDto;
import com.wegive.domain.request.entity.Request;
import com.wegive.domain.request.repository.RequestRepository;
import com.wegive.domain.user.entity.User;
import com.wegive.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * [Service] 나눔 신청 및 당첨자 선정 로직
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RequestService {

    private final RequestRepository requestRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    /**
     * 기능: 나눔 신청하기 (저요!)
     * 검증: 1.자기 물건 신청 불가 2.중복 신청 불가
     */
    public Long createRequest(Long userId, RequestCreateDto dto) {
        // 1. 신청자 및 상품 조회
        User buyer = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("회원 없음"));
        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new RuntimeException("상품 없음"));

        // 2. [검증] 나눔이가 본인 물건 신청 불가
        if (item.getSeller().getUserId().equals(userId)) {
            throw new RuntimeException("자신의 물건은 신청할 수 없습니다.");
        }

        // 3. [검증] 이미 신청했는지 확인 (중복 방지)
        if (requestRepository.existsByItemAndBuyer(item, buyer)) {
            throw new RuntimeException("이미 신청한 상품입니다.");
        }

        // 4. 저장 (상태는 기본값 WAITING)
        Request request = Request.builder()
                .item(item)
                .buyer(buyer)
                .reqStatus("WAITING")
                .build();

        requestRepository.save(request);
        return request.getReqId();
    }

    /**
     * 기능: [마이페이지] 내가 신청한 목록 조회
     */
    @Transactional(readOnly = true)
    public List<RequestResponseDto> getMyRequests(Long userId) {
        User buyer = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("회원 없음"));

        return requestRepository.findByBuyerOrderByReqTimeDesc(buyer).stream()
                .map(RequestResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 기능: [나눔이용] 내 물건에 들어온 신청자 목록 조회 (선착순 정렬)
     */
    @Transactional(readOnly = true)
    public List<RequestResponseDto> getRequestsByItem(Long userId, Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("상품 없음"));

        // 본인이 나눔이인지 확인 (보안)
        if (!item.getSeller().getUserId().equals(userId)) {
            throw new RuntimeException("나눔이만 신청 목록을 볼 수 있습니다.");
        }

        return requestRepository.findByItemOrderByReqTimeAsc(item).stream()
                .map(RequestResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 기능: [나눔이용] 당첨자 선정 (나눔 확정)
     * 로직: 선택된 사람은 ACCEPTED, 나머지는 거절 처리? (일단 선택된 사람만 변경)
     */
    public void acceptRequest(Long userId, Long reqId) {
        Request request = requestRepository.findById(reqId)
                .orElseThrow(() -> new RuntimeException("신청 내역 없음"));
        Item item = request.getItem();

        // 1. 권한 확인
        if (!item.getSeller().getUserId().equals(userId)) {
            throw new RuntimeException("나눔이만 당첨자를 선정할 수 있습니다.");
        }

        // 2. 상태 변경 (WAITING -> ACCEPTED)
        request.changeStatus("ACCEPTED");

        // 3. [추가된 로직] 상품 상태도 '예약중'으로 변경 (다른 사람이 못 가져가게)
        // Item Entity에 changeStatus 메서드 추가 필요
        // item.changeStatus("RESERVED");
    }
}