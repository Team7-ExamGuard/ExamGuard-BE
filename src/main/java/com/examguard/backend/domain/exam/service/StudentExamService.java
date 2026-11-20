package com.examguard.backend.domain.exam.service;

import com.examguard.backend.domain.attempt.entity.Attempt;
import com.examguard.backend.domain.attempt.entity.AttemptStatus;
import com.examguard.backend.domain.attempt.repository.AttemptRepository;
import com.examguard.backend.domain.exam.dto.AnswersResponse;
import com.examguard.backend.domain.exam.dto.JoinResponse;
import com.examguard.backend.domain.exam.dto.SaveAnswersRequest;
import com.examguard.backend.domain.exam.dto.SaveAnswersResponse;
import com.examguard.backend.domain.exam.dto.StudentExamResponse;
import com.examguard.backend.domain.exam.dto.StudentQuestionResponse;
import com.examguard.backend.domain.exam.entity.Exam;
import com.examguard.backend.domain.exam.entity.ExamStatus;
import com.examguard.backend.domain.exam.repository.ExamRepository;
import com.examguard.backend.domain.exam.repository.QuestionRepository;
import com.examguard.backend.global.exception.ExamException;
import com.examguard.backend.global.payload.status.ErrorStatus;
import com.examguard.backend.global.util.TimeUtils;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 학생 응시: 시험 코드로 입장 → 시험 정보 조회 → 문항 조회(응시 시작) → 답안 저장·복원.
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

    // 시작 시각이 되면 문항을 정답 없이 준다. 처음 받으면 응시 중(IN_PROGRESS)이 된다
    public List<StudentQuestionResponse> getQuestions(Long userId, Long examId) {
        Instant now = Instant.now();
        Attempt attempt = findMyAttempt(userId, examId, now);
        if (attempt.getStatus() == AttemptStatus.SUBMITTED) {
            throw new ExamException(ErrorStatus.ALREADY_SUBMITTED);
        }
        ExamStatus status = attempt.getExam().statusAt(now);
        if (status == ExamStatus.PUBLISHED) {
            throw new ExamException(ErrorStatus.EXAM_NOT_STARTED); // 프론트는 잠시 뒤 다시 요청
        }
        if (status == ExamStatus.CLOSED) {
            throw new ExamException(ErrorStatus.EXAM_CLOSED);
        }

        if (attempt.getStatus() == AttemptStatus.WAITING) {
            attempt.start(now); // 새로고침으로 다시 받아도 처음 시작 시각은 그대로
        }
        return questionRepository.findByExamIdOrderBySeq(examId).stream()
                .map(StudentQuestionResponse::from)
                .toList();
    }

    // 새로고침·재접속 때 답안 복원용
    public AnswersResponse getAnswers(Long userId, Long examId) {
        return AnswersResponse.from(findMyAttempt(userId, examId, Instant.now()));
    }

    // 답안 자동 저장 (입력 후 2~3초마다). 종료 시각이 되면 바로 막는다 (유예는 최종 제출에만)
    public SaveAnswersResponse saveAnswers(Long userId, Long examId, SaveAnswersRequest request) {
        Instant now = Instant.now();
        Attempt attempt = findMyAttempt(userId, examId, now);
        if (attempt.getStatus() == AttemptStatus.SUBMITTED) {
            throw new ExamException(ErrorStatus.ALREADY_SUBMITTED);
        }
        if (attempt.getStatus() == AttemptStatus.WAITING) {
            throw new ExamException(ErrorStatus.EXAM_NOT_STARTED);
        }
        if (!now.isBefore(attempt.getExam().getEndAt())) {
            throw new ExamException(ErrorStatus.EXAM_CLOSED, "시험 시간이 끝났습니다.");
        }

        Map<String, Object> answers = request.answers() != null ? request.answers() : new HashMap<>();
        int saveFailCount = request.saveFailCount() != null ? request.saveFailCount() : 0;
        int updated = attemptRepository.saveAnswersIfNewer(attempt.getId(), request.version(), answers,
                Attempt.countAnswered(answers), saveFailCount, now);
        if (updated == 0) {
            throw new ExamException(ErrorStatus.STALE_VERSION); // 프론트는 무시하고 재시도하지 않음
        }
        return new SaveAnswersResponse(request.version(), TimeUtils.toKst(now));
    }

    // 학생 API 공통: 입장하지 않은 시험은 없는 시험과 똑같이 404
    private Attempt findMyAttempt(Long userId, Long examId, Instant now) {
        Attempt attempt = attemptRepository.findByExamIdAndUserId(examId, userId)
                .orElseThrow(() -> new ExamException(ErrorStatus.EXAM_NOT_FOUND));
        attempt.submitIfExpired(now);
        return attempt;
    }
}
