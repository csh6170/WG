package com.wegive.domain.item.repository;

import com.wegive.domain.item.entity.ItemImage;
import com.wegive.domain.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * [Repository] 상품 이미지 조회
 */
public interface ItemImageRepository extends JpaRepository<ItemImage, Long> {

    // 특정 상품의 모든 이미지 가져오기
    List<ItemImage> findByItemOrderByImgIdAsc(Item item);

    // 목록용: 특정 상품의 '대표 이미지(Y)'만 가져오기
    List<ItemImage> findByItemAndIsThumbnail(Item item, String isThumbnail);

    // [추가] 특정 물건의 이미지 싹 지우기
    void deleteByItem(Item item);
}