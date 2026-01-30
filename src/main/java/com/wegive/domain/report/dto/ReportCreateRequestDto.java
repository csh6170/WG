package com.wegive.domain.report.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportCreateRequestDto {
    private Long reportedUserId; // 신고 당하는 사람 ID
    private Long itemId;         // (선택) 관련 상품 ID
    private String reason;       // 사유 (예: 비매너, 사기 등)
    private String description;  // 상세 내용
}