package com.examguard.backend.domain.exam.dto;

import jakarta.validation.constraints.NotBlank;

// 학생 입장: 관리자가 공개할 때 발급한 6자리 시험 코드
public record JoinRequest(
        @NotBlank(message = "시험 코드를 입력하세요.") String code
) {
}
