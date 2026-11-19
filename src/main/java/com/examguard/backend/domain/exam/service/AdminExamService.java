package com.examguard.backend.domain.exam.service;

import com.examguard.backend.domain.attempt.repository.AttemptRepository;
import com.examguard.backend.domain.exam.dto.ExamDetailResponse;
import com.examguard.backend.domain.exam.dto.ExamIdResponse;
import com.examguard.backend.domain.exam.dto.ExamSaveRequest;
import com.examguard.backend.domain.exam.dto.ExamSummaryResponse;
import com.examguard.backend.domain.exam.dto.QuestionSaveRequest;
import com.examguard.backend.domain.exam.entity.Exam;
import com.examguard.backend.domain.exam.entity.Question;
import com.examguard.backend.domain.exam.entity.QuestionType;
import com.examguard.backend.domain.exam.repository.ExamRepository;
import com.examguard.backend.domain.exam.repository.QuestionRepository;
import com.examguard.backend.domain.user.repository.UserRepository;
import com.examguard.backend.global.exception.ExamException;
import com.examguard.backend.global.payload.status.ErrorStatus;
import com.examguard.backend.global.util.TimeUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 시험 출제: 생성·임시 저장(DRAFT) → 공개(코드 발급) → 조기 종료.
 * 공개 검증 순서와 문구는 프론트 mock(src/api/mock/index.js)과 같게 맞춘다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminExamService {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final AttemptRepository attemptRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ExamSummaryResponse> getExams() {
        Instant now = Instant.now();
        Map<Long, Long> joinedCounts = attemptRepository.countJoinedByExam().stream()
                .collect(Collectors.toMap(AttemptRepository.JoinedCount::getExamId,
                        AttemptRepository.JoinedCount::getJoinedCount));

        return examRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream() // 최근에 만든 시험부터
                .map(exam -> ExamSummaryResponse.of(exam, now, joinedCounts.getOrDefault(exam.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExamDetailResponse getExam(Long examId) {
        Exam exam = findExam(examId);
        List<Question> questions = questionRepository.findByExamIdOrderBySeq(examId);
        return ExamDetailResponse.of(exam, questions, Instant.now());
    }

    public ExamIdResponse createExam(Long adminId, ExamSaveRequest request) {
        Exam exam = examRepository.save(Exam.builder()
                .title(emptyIfNull(request.title()))
                .startAt(TimeUtils.toInstant(request.startAt()))
                .endAt(TimeUtils.toInstant(request.endAt()))
                .createdBy(userRepository.getReferenceById(adminId))
                .build());
        saveQuestions(exam, request.questions());
        return new ExamIdResponse(exam.getId());
    }

    // 임시 저장: 제목·시각을 바꾸고 문항은 통째로 다시 만든다 (DRAFT 문항은 다른 데이터가 참조하지 않음)
    public ExamIdResponse updateExam(Long examId, ExamSaveRequest request) {
        Exam exam = findExam(examId);
        if (exam.isPublished()) {
            throw new ExamException(ErrorStatus.EXAM_NOT_DRAFT);
        }

        exam.updateDraft(emptyIfNull(request.title()),
                TimeUtils.toInstant(request.startAt()),
                TimeUtils.toInstant(request.endAt()));
        questionRepository.deleteAllByExamId(examId);
        saveQuestions(exam, request.questions());
        return new ExamIdResponse(examId);
    }

    private Exam findExam(Long examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new ExamException(ErrorStatus.EXAM_NOT_FOUND));
    }

    private void saveQuestions(Exam exam, List<QuestionSaveRequest> requests) {
        if (requests == null) {
            return;
        }
        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            QuestionSaveRequest request = requests.get(i);
            boolean multipleChoice = request.type() == QuestionType.MULTIPLE_CHOICE;
            questions.add(Question.builder()
                    .exam(exam)
                    .seq(i + 1) // 배열 순서가 문항 순서
                    .type(request.type())
                    .content(emptyIfNull(request.content()))
                    // 객관식인데 보기가 없으면 빈 칸 4개, 단답형은 보기 없음
                    .options(multipleChoice
                            ? (request.options() != null ? request.options() : Arrays.asList("", "", "", ""))
                            : null)
                    .answer(toStoredAnswer(request.answer()))
                    .build());
        }
        questionRepository.saveAll(questions);
    }

    // 정답은 문자열로 저장 (객관식 2 → "2", 단답형 "정답" → "정답")
    private String toStoredAnswer(Object answer) {
        if (answer == null) {
            return null;
        }
        String value = (answer instanceof Number number) ? String.valueOf(number.longValue()) : String.valueOf(answer);
        if (value.length() > 255) {
            throw new ExamException(ErrorStatus.VALIDATION_ERROR, "정답은 255자 이하로 입력하세요.");
        }
        return value;
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
