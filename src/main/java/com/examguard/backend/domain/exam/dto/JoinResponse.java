package com.examguard.backend.domain.exam.dto;

import com.examguard.backend.domain.attempt.entity.AttemptStatus;

// 프론트는 attemptStatus로 이동할 화면을 정한다 (WAITING → 대기, IN_PROGRESS → 응시)
public record JoinResponse(
        Long examId,
        AttemptStatus attemptStatus
) {
}
