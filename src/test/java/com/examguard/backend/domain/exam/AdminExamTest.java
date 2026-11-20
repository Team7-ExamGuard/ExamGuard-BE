package com.examguard.backend.domain.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examguard.backend.domain.attempt.entity.Attempt;
import com.examguard.backend.domain.attempt.entity.AttemptStatus;
import com.examguard.backend.domain.attempt.repository.AttemptRepository;
import com.examguard.backend.domain.exam.entity.Exam;
import com.examguard.backend.domain.exam.repository.ExamRepository;
import com.examguard.backend.domain.user.entity.Role;
import com.examguard.backend.domain.user.entity.User;
import com.examguard.backend.domain.user.repository.UserRepository;
import com.examguard.backend.global.security.jwt.JwtTokenProvider;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 시험 출제 흐름: 생성 → 임시 저장 → 공개 → 응시 현황 → 조기 종료
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 테스트에서 만든 데이터는 끝나면 지워짐
class AdminExamTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ExamRepository examRepository;
    @Autowired AttemptRepository attemptRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired EntityManager entityManager;

    User admin;
    String adminToken;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(User.builder()
                .loginId("test-admin").password("pw").name("원장").role(Role.ADMIN).build());
        adminToken = "Bearer " + jwtTokenProvider.createAccessToken(admin.getId(), Role.ADMIN);
    }

    @Test
    void 시험을_만들고_상세를_조회한다() throws Exception {
        Long examId = createExam(fullExamJson());

        mockMvc.perform(get("/api/admin/exams/" + examId).header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("DRAFT"))
                .andExpect(jsonPath("$.result.code").isEmpty())
                .andExpect(jsonPath("$.result.startAt", endsWith("+09:00")))  // 한국 시각으로 응답
                .andExpect(jsonPath("$.result.questions[0].order").value(1))
                .andExpect(jsonPath("$.result.questions[0].answer").value(2))     // 객관식 정답은 숫자
                .andExpect(jsonPath("$.result.questions[1].answer").value("1/3")); // 단답형 정답은 문자열
    }

    @Test
    void 임시_저장하면_문항이_통째로_바뀐다() throws Exception {
        Long examId = createExam(fullExamJson());

        mockMvc.perform(put("/api/admin/exams/" + examId).header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "제목 수정", "questions": [
                                  {"type": "SHORT_ANSWER", "content": "새 문항", "answer": "정답"}
                                ]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/exams/" + examId).header("Authorization", adminToken))
                .andExpect(jsonPath("$.result.title").value("제목 수정"))
                .andExpect(jsonPath("$.result.questions.length()").value(1))
                .andExpect(jsonPath("$.result.questions[0].content").value("새 문항"));
    }

    @Test
    void 공개할_때_검증에_걸리면_400() throws Exception {
        Long examId = createExam("{\"title\": \"\", \"questions\": []}");

        mockMvc.perform(post("/api/admin/exams/" + examId + "/publish").header("Authorization", adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("제목을 입력하세요."));
    }

    @Test
    void 공개하면_코드가_발급되고_더는_수정할_수_없다() throws Exception {
        Long examId = createExam(fullExamJson());

        mockMvc.perform(post("/api/admin/exams/" + examId + "/publish").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.code", matchesPattern("[A-Z0-9]{6}")));

        mockMvc.perform(put("/api/admin/exams/" + examId).header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(fullExamJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXAM_NOT_DRAFT"));
    }

    @Test
    void 조기_종료하면_응시_중인_학생은_제출된다() throws Exception {
        Long examId = createExam(fullExamJson());
        mockMvc.perform(post("/api/admin/exams/" + examId + "/publish").header("Authorization", adminToken));
        Long attemptId = saveInProgressAttempt(examId);

        mockMvc.perform(post("/api/admin/exams/" + examId + "/close").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.endAt").isNotEmpty());

        // 테스트 전체가 한 트랜잭션이라, 변경을 DB에 반영(flush)한 뒤 메모리를 비우고 다시 읽는다
        entityManager.flush();
        entityManager.clear();
        assertThat(attemptRepository.findById(attemptId).orElseThrow().getStatus()).isEqualTo(AttemptStatus.SUBMITTED);

        // 이미 종료된 시험은 다시 종료할 수 없음
        mockMvc.perform(post("/api/admin/exams/" + examId + "/close").header("Authorization", adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXAM_NOT_OPEN"));
    }

    @Test
    void 목록에_입장_인원이_나온다() throws Exception {
        Long examId = createExam(fullExamJson());
        saveInProgressAttempt(examId);

        mockMvc.perform(get("/api/admin/exams").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].id").value(examId)) // 최근에 만든 시험이 맨 위
                .andExpect(jsonPath("$.result[0].joinedCount").value(1));
    }

    @Test
    void 응시_현황에_학생별_상태와_요약이_나온다() throws Exception {
        Exam exam = savePublishedExam(Duration.ofMinutes(-10), Duration.ofMinutes(50)); // 진행 중
        saveAttempt(exam, "test-student1", AttemptStatus.WAITING);
        Attempt inProgress = saveAttempt(exam, "test-student2", AttemptStatus.IN_PROGRESS);
        saveAttempt(exam, "test-student3", AttemptStatus.SUBMITTED);
        // 2문항 답하고, 저장에 1번 실패했던 학생
        attemptRepository.saveAnswersIfNewer(inProgress.getId(), 1, Map.of("11", 2, "12", "1/3"), 2, 1, Instant.now());

        mockMvc.perform(get("/api/admin/exams/" + exam.getId() + "/status").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("OPEN"))
                .andExpect(jsonPath("$.result.summary.waiting").value(1))
                .andExpect(jsonPath("$.result.summary.inProgress").value(1))
                .andExpect(jsonPath("$.result.summary.submitted").value(1))
                .andExpect(jsonPath("$.result.summary.saveFailed").value(1))
                .andExpect(jsonPath("$.result.students[1].loginId").value("test-student2")) // 입장한 순서
                .andExpect(jsonPath("$.result.students[1].answeredCount").value(2))
                .andExpect(jsonPath("$.result.students[1].saveFailCount").value(1))
                .andExpect(jsonPath("$.result.students[1].lastSavedAt", endsWith("+09:00")));
    }

    @Test
    void 종료_후_유예가_지나면_응시_중인_학생은_제출로_나온다() throws Exception {
        Exam exam = savePublishedExam(Duration.ofMinutes(-60), Duration.ofMinutes(-1)); // 1분 전 종료
        saveAttempt(exam, "test-student1", AttemptStatus.IN_PROGRESS);

        mockMvc.perform(get("/api/admin/exams/" + exam.getId() + "/status").header("Authorization", adminToken))
                .andExpect(jsonPath("$.result.status").value("CLOSED"))
                .andExpect(jsonPath("$.result.summary.inProgress").value(0))
                .andExpect(jsonPath("$.result.summary.submitted").value(1))
                .andExpect(jsonPath("$.result.students[0].status").value("SUBMITTED"));
    }

    // ---------- 도우미 ----------

    // 공개 검증을 통과하는 시험 본문 (내일 10시~11시, 객관식 1 + 단답형 1)
    private String fullExamJson() {
        OffsetDateTime start = OffsetDateTime.now(ZoneId.of("Asia/Seoul"))
                .plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        return """
                {"title": "중2 수학 1단원 평가", "startAt": "%s", "endAt": "%s", "questions": [
                  {"type": "MULTIPLE_CHOICE", "content": "다음 중 유리수가 아닌 것은?", "options": ["0.5", "-3", "√2", "2/3"], "answer": 2},
                  {"type": "SHORT_ANSWER", "content": "0.333…을 기약분수로 나타내면?", "options": null, "answer": "1/3"}
                ]}
                """.formatted(start, start.plusHours(1));
    }

    private Long createExam(String json) throws Exception {
        String body = mockMvc.perform(post("/api/admin/exams").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.result.id")).longValue();
    }

    // 공개된 시험 (지금 기준으로 시작·종료 시각을 정함)
    private Exam savePublishedExam(Duration startFromNow, Duration endFromNow) {
        Instant now = Instant.now();
        Exam exam = Exam.builder().title("중2 수학 1단원 평가")
                .startAt(now.plus(startFromNow)).endAt(now.plus(endFromNow)).createdBy(admin).build();
        exam.publish("TEST01", now.minus(Duration.ofDays(1)));
        return examRepository.save(exam);
    }

    // 학생 한 명이 응시 중인 상태를 만든다
    private Long saveInProgressAttempt(Long examId) {
        Exam exam = examRepository.findById(examId).orElseThrow();
        return saveAttempt(exam, "test-student", AttemptStatus.IN_PROGRESS).getId();
    }

    // 학생 한 명이 입장한 응시를 만든다 (WAITING → 시작하면 IN_PROGRESS → 제출하면 SUBMITTED)
    private Attempt saveAttempt(Exam exam, String loginId, AttemptStatus status) {
        User student = userRepository.save(User.builder()
                .loginId(loginId).password("pw").name("학생").role(Role.STUDENT).build());
        Attempt attempt = Attempt.builder().exam(exam).user(student).joinedAt(Instant.now()).build();
        if (status != AttemptStatus.WAITING) {
            attempt.start(Instant.now());
        }
        if (status == AttemptStatus.SUBMITTED) {
            attempt.submit(Instant.now());
        }
        attemptRepository.save(attempt);
        entityManager.flush();
        return attempt;
    }
}
