package com.examguard.backend.domain.exam.dto;

import com.examguard.backend.domain.exam.entity.Exam;
import com.examguard.backend.domain.exam.entity.ExamStatus;
import com.examguard.backend.global.util.TimeUtils;
import java.time.Instant;
import java.time.OffsetDateTime;

// 관리자 시험 목록의 한 줄
public record ExamSummaryResponse(
        Long id,
        String title,
        ExamStatus status,
        String code,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        long joinedCount
) {

    public static ExamSummaryResponse of(Exam exam, Instant now, long joinedCount) {
        return new ExamSummaryResponse(
                exam.getId(),
                exam.getTitle(),
                exam.statusAt(now),
                exam.getCode(),
                TimeUtils.toKst(exam.getStartAt()),
                TimeUtils.toKst(exam.getEndAt()),
                joinedCount);
    }
}
