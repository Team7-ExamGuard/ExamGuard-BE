package com.examguard.backend.domain.exam.dto;

import com.examguard.backend.domain.exam.entity.Exam;
import com.examguard.backend.domain.exam.entity.ExamStatus;
import com.examguard.backend.domain.exam.entity.Question;
import com.examguard.backend.global.util.TimeUtils;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

// 관리자 시험 상세 (정답 포함)
public record ExamDetailResponse(
        Long id,
        String title,
        ExamStatus status,
        String code,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        List<QuestionDetailResponse> questions
) {

    public static ExamDetailResponse of(Exam exam, List<Question> questions, Instant now) {
        return new ExamDetailResponse(
                exam.getId(),
                exam.getTitle(),
                exam.statusAt(now),
                exam.getCode(),
                TimeUtils.toKst(exam.getStartAt()),
                TimeUtils.toKst(exam.getEndAt()),
                questions.stream().map(QuestionDetailResponse::from).toList());
    }
}
