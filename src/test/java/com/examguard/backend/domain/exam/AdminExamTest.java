package com.examguard.backend.domain.exam;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examguard.backend.domain.attempt.entity.Attempt;
import com.examguard.backend.domain.attempt.repository.AttemptRepository;
import com.examguard.backend.domain.exam.entity.Exam;
import com.examguard.backend.domain.exam.repository.ExamRepository;
import com.examguard.backend.domain.user.entity.Role;
import com.examguard.backend.domain.user.entity.User;
import com.examguard.backend.domain.user.repository.UserRepository;
import com.examguard.backend.global.security.jwt.JwtTokenProvider;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 시험 출제 흐름: 생성 → 임시 저장 → 공개 → 조기 종료
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

    String adminToken;

    @BeforeEach
    void setUp() {
        User admin = userRepository.save(User.builder()
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
    void 목록에_입장_인원이_나온다() throws Exception {
        Long examId = createExam(fullExamJson());
        saveInProgressAttempt(examId);

        mockMvc.perform(get("/api/admin/exams").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].id").value(examId)) // 최근에 만든 시험이 맨 위
                .andExpect(jsonPath("$.result[0].joinedCount").value(1));
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

    // 학생 한 명이 입장해서 응시 중인 상태를 만든다
    private Long saveInProgressAttempt(Long examId) {
        Exam exam = examRepository.findById(examId).orElseThrow();
        User student = userRepository.save(User.builder()
                .loginId("test-student").password("pw").name("학생").role(Role.STUDENT).build());
        Attempt attempt = Attempt.builder().exam(exam).user(student).joinedAt(Instant.now()).build();
        attempt.start(Instant.now());
        attemptRepository.save(attempt);
        entityManager.flush();
        return attempt.getId();
    }
}
