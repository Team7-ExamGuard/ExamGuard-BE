package com.examguard.backend.domain.exam.service;

import com.examguard.backend.domain.attempt.entity.Attempt;
import com.examguard.backend.domain.attempt.entity.AttemptStatus;
import com.examguard.backend.domain.attempt.repository.AttemptRepository;
import com.examguard.backend.domain.exam.dto.JoinResponse;
import com.examguard.backend.domain.exam.dto.StudentExamResponse;
import com.examguard.backend.domain.exam.entity.Exam;
import com.examguard.backend.domain.exam.entity.ExamStatus;
import com.examguard.backend.domain.exam.repository.ExamRepository;
import com.examguard.backend.domain.exam.repository.QuestionRepository;
import com.examguard.backend.global.exception.ExamException;
import com.examguard.backend.global.payload.status.ErrorStatus;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 학생 응시: 시험 코드로 입장 → 시험 정보 조회.
 * 에러 판단 순서와 문구는 프론트 mock(src/api/mock/index.js)과 같게 맞춘다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StudentExamService {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
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

    public StudentExamResponse getExam(Long userId, Long examId) {
        Instant now = Instant.now();
        Attempt attempt = findMyAttempt(userId, examId, now);
        return StudentExamResponse.of(attempt, questionRepository.countByExamId(examId), now);
    }

    // 학생 API 공통: 입장하지 않은 시험은 없는 시험과 똑같이 404
    private Attempt findMyAttempt(Long userId, Long examId, Instant now) {
        Attempt attempt = attemptRepository.findByExamIdAndUserId(examId, userId)
                .orElseThrow(() -> new ExamException(ErrorStatus.EXAM_NOT_FOUND));
        attempt.submitIfExpired(now);
        return attempt;
    }
}
