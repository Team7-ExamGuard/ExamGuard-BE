package com.examguard.backend.domain.exam.dto;

import com.examguard.backend.domain.attempt.entity.Attempt;
import com.examguard.backend.global.util.TimeUtils;
import java.time.OffsetDateTime;
import java.util.Map;

// 서버에 저장된 답안. 저장한 적 없으면 { version: 0, answers: {}, savedAt: null }
// 프론트는 브라우저에 남은 답안과 version을 비교해 높은 쪽으로 복원한다
public record AnswersResponse(
        int version,
        Map<String, Object> answers,
        OffsetDateTime savedAt
) {

    public static AnswersResponse from(Attempt attempt) {
        return new AnswersResponse(
                attempt.getVersion(),
                attempt.getAnswers(),
                TimeUtils.toKst(attempt.getLastSavedAt()));
    }
}
