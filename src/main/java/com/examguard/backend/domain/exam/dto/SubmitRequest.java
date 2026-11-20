package com.examguard.backend.domain.exam.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

// 최종 제출 (직접 제출·시간 종료 자동 제출 공용). 프론트가 가진 마지막 답안과 version을 같이 보낸다
public record SubmitRequest(
        @NotNull(message = "version을 입력하세요.") Integer version,
        Map<String, Object> answers
) {
}
