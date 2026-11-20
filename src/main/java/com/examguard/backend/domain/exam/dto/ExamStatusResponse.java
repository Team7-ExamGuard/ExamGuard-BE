package com.examguard.backend.domain.exam.dto;

import com.examguard.backend.domain.attempt.entity.AttemptStatus;
import com.examguard.backend.domain.attempt.repository.AttemptRepository;
import com.examguard.backend.domain.exam.entity.Exam;
import com.examguard.backend.domain.exam.entity.ExamStatus;
import com.examguard.backend.global.util.TimeUtils;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

// 관리자 응시 현황 (5초 폴링). 학생은 입장한 순서대로
public record ExamStatusResponse(
        Long id,
        String title,
        ExamStatus status,
        String code,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        Summary summary,
        List<StudentStatus> students
) {

    public static ExamStatusResponse of(Exam exam, Instant now, List<StudentStatus> students) {
        return new ExamStatusResponse(
                exam.getId(),
                exam.getTitle(),
                exam.statusAt(now),
                exam.getCode(),
                TimeUtils.toKst(exam.getStartAt()),
                TimeUtils.toKst(exam.getEndAt()),
                Summary.of(students),
                students);
    }

    // saveFailed = 저장에 한 번이라도 실패한 학생 수
    public record Summary(long waiting, long inProgress, long submitted, long saveFailed) {

        static Summary of(List<StudentStatus> students) {
            return new Summary(
                    count(students, AttemptStatus.WAITING),
                    count(students, AttemptStatus.IN_PROGRESS),
                    count(students, AttemptStatus.SUBMITTED),
                    students.stream().filter(student -> student.saveFailCount() > 0).count());
        }

        private static long count(List<StudentStatus> students, AttemptStatus status) {
            return students.stream().filter(student -> student.status() == status).count();
        }
    }

    public record StudentStatus(
            String loginId,
            String name,
            AttemptStatus status,
            OffsetDateTime joinedAt,
            OffsetDateTime lastSavedAt,
            int answeredCount,
            int saveFailCount
    ) {

        public static StudentStatus from(AttemptRepository.StudentRow row) {
            return new StudentStatus(
                    row.getLoginId(),
                    row.getName(),
                    row.getStatus(),
                    TimeUtils.toKst(row.getJoinedAt()),
                    TimeUtils.toKst(row.getLastSavedAt()),
                    row.getAnsweredCount(),
                    row.getSaveFailCount());
        }
    }
}
