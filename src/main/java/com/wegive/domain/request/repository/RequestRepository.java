package com.wegive.domain.request.repository;

import com.wegive.domain.request.entity.Request;
import com.wegive.domain.item.entity.Item;
import com.wegive.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * [Repository] 나눔 신청 내역 조회
 */
public interface RequestRepository extends JpaRepository<Request, Long> {

    // 중복 신청 방지 체크용 (이미 신청했니?)
    boolean existsByItemAndBuyer(Item item, User buyer);

    // [마이페이지] 내가 신청한 내역
    List<Request> findByBuyerOrderByReqTimeDesc(User buyer);

    // [나눔이용] 내 상품에 들어온 신청자 목록 (선착순이니까 시간 오름차순 Asc)
    List<Request> findByItemOrderByReqTimeAsc(Item item);

    // [추가] 특정 상품에 대한 모든 신청 삭제 (회원 탈퇴 시 필요)
    void deleteByItem(Item item);

    // [추가] 내가 '구매자'로서 신청한 내역 삭제 (회원탈퇴용)
    void deleteByBuyer(User buyer);
}