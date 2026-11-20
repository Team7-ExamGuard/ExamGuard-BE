package com.examguard.backend.domain.exam.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Map;

// 답안 자동 저장. answers는 전체를 보낸다 { "문항id": 보기 인덱스(0~3) 또는 문자열 }
// version은 답안을 바꿀 때마다 프론트가 +1, saveFailCount는 지난 성공 이후 실패한 저장 횟수
public record SaveAnswersRequest(
        @NotNull(message = "version을 입력하세요.") Integer version,
        Map<String, Object> answers,
        @PositiveOrZero(message = "saveFailCount는 0 이상이어야 합니다.") Integer saveFailCount
) {
}
