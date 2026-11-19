package com.examguard.backend.domain.exam.service;

import com.examguard.backend.domain.attempt.entity.Attempt;
import com.examguard.backend.domain.attempt.entity.AttemptStatus;
import com.examguard.backend.domain.attempt.repository.AttemptRepository;
import com.examguard.backend.domain.exam.dto.JoinResponse;
import com.examguard.backend.domain.exam.entity.Exam;
import com.examguard.backend.domain.exam.entity.ExamStatus;
import com.examguard.backend.domain.exam.repository.ExamRepository;
import com.examguard.backend.global.exception.ExamException;
import com.examguard.backend.global.payload.status.ErrorStatus;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 학생 응시: 시험 코드로 입장.
 * 에러 판단 순서와 문구는 프론트 mock(src/api/mock/index.js)과 같게 맞춘다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StudentExamService {

    private final ExamRepository examRepository;
    private final AttemptRepository attemptRepository;

    // 이미 입장한 시험이면 기존 응시 상태를 그대로 돌려준다 (에러 아님)
    public JoinResponse joinExam(Long userId, String code) {
        Exam exam = examRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new ExamException(ErrorStatus.EXAM_NOT_FOUND, "존재하지 않는 시험 코드입니다."));
        Instant now = Instant.now();
        ExamStatus status = exam.statusAt(now);
        if (status == ExamStatus.DRAFT) {
            throw new ExamException(ErrorStatus.EXAM_NOT_PUBLISHED);
        }

        Optional<Attempt> existing = attemptRepository.findByExamIdAndUserId(exam.getId(), userId);
        existing.ifPresent(attempt -> attempt.submitIfExpired(now));
        if (existing.isPresent() && existing.get().getStatus() == AttemptStatus.SUBMITTED) {
            throw new ExamException(ErrorStatus.ALREADY_SUBMITTED);
        }
        if (status == ExamStatus.CLOSED) {
            throw new ExamException(ErrorStatus.EXAM_CLOSED);
        }

        if (existing.isPresent()) {
            return new JoinResponse(exam.getId(), existing.get().getStatus());
        }
        attemptRepository.insertIfAbsent(exam.getId(), userId, now);
        Attempt attempt = attemptRepository.findByExamIdAndUserId(exam.getId(), userId).orElseThrow();
        return new JoinResponse(exam.getId(), attempt.getStatus());
    }
}
