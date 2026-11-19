package com.examguard.backend.domain.exam.dto;

import com.examguard.backend.domain.attempt.entity.Attempt;
import com.examguard.backend.domain.attempt.entity.AttemptStatus;
import com.examguard.backend.domain.exam.entity.Exam;
import com.examguard.backend.domain.exam.entity.ExamStatus;
import com.examguard.backend.global.util.TimeUtils;
import java.time.Instant;
import java.time.OffsetDateTime;

// 학생 시험 정보 (대기·응시·완료 화면 공통). 제한 시간 = endAt - startAt
public record StudentExamResponse(
        Long id,
        String title,
        ExamStatus status,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        long questionCount,
        AttemptInfo attempt
) {

    // 내 응시 상태
    public record AttemptInfo(AttemptStatus status, OffsetDateTime submittedAt) {
    }

    public static StudentExamResponse of(Attempt attempt, long questionCount, Instant now) {
        Exam exam = attempt.getExam();
        return new StudentExamResponse(
                exam.getId(),
                exam.getTitle(),
                exam.statusAt(now),
                TimeUtils.toKst(exam.getStartAt()),
                TimeUtils.toKst(exam.getEndAt()),
                questionCount,
                new AttemptInfo(attempt.getStatus(), TimeUtils.toKst(attempt.getSubmittedAt())));
    }
}
