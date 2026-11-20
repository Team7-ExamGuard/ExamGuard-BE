package com.examguard.backend.domain.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examguard.backend.domain.attempt.entity.Attempt;
import com.examguard.backend.domain.attempt.entity.AttemptStatus;
import com.examguard.backend.domain.attempt.repository.AttemptRepository;
import com.examguard.backend.domain.exam.entity.Exam;
import com.examguard.backend.domain.exam.entity.Question;
import com.examguard.backend.domain.exam.entity.QuestionType;
import com.examguard.backend.domain.exam.repository.ExamRepository;
import com.examguard.backend.domain.exam.repository.QuestionRepository;
import com.examguard.backend.domain.user.entity.Role;
import com.examguard.backend.domain.user.entity.User;
import com.examguard.backend.domain.user.repository.UserRepository;
import com.examguard.backend.global.security.jwt.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * 학생 응시 흐름: 시험 코드로 입장 → 시험 정보 조회 → 문항 조회(응시 시작) → 답안 저장·복원 → 최종 제출
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 테스트에서 만든 데이터는 끝나면 지워짐
class StudentExamTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ExamRepository examRepository;
    @Autowired QuestionRepository questionRepository;
    @Autowired AttemptRepository attemptRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired EntityManager entityManager;

    User admin;
    User student;
    String studentToken;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(User.builder()
                .loginId("test-admin").password("pw").name("원장").role(Role.ADMIN).build());
        student = userRepository.save(User.builder()
                .loginId("test-student").password("pw").name("학생").role(Role.STUDENT).build());
        studentToken = "Bearer " + jwtTokenProvider.createAccessToken(student.getId(), Role.STUDENT);
    }

    @Test
    void 코드로_입장하면_대기_상태가_되고_다시_입장해도_한_건이다() throws Exception {
        Exam exam = savePublishedExam("TEST01", Duration.ofMinutes(10), Duration.ofMinutes(70)); // 10분 뒤 시작

        join(" test01 ") // 앞뒤 공백·소문자도 받아 줌
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.examId").value(exam.getId()))
                .andExpect(jsonPath("$.result.attemptStatus").value("WAITING"));

        join("TEST01").andExpect(status().isOk()); // 이미 입장했어도 에러 아님
        assertThat(attemptRepository.findAll().stream()
                .filter(a -> a.getExam().getId().equals(exam.getId()))).hasSize(1);
    }

    @Test
    void 없는_코드면_404() throws Exception {
        join("NOPE99")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXAM_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 시험 코드입니다."));
    }

    @Test
    void 끝난_시험이면_409() throws Exception {
        savePublishedExam("TEST01", Duration.ofMinutes(-70), Duration.ofMinutes(-10)); // 10분 전에 끝남

        join("TEST01")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXAM_CLOSED"));
    }

    @Test
    void 응시_중에_시험이_끝나고_유예가_지나면_제출된_것으로_본다() throws Exception {
        Exam exam = savePublishedExam("TEST01", Duration.ofMinutes(-70), Duration.ofMinutes(-10));
        saveAttempt(exam, true); // 응시 중이던 학생

        join("TEST01")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_SUBMITTED"));
    }

    @Test
    void 입장한_시험의_정보와_내_응시_상태를_본다() throws Exception {
        Exam exam = savePublishedExam("TEST01", Duration.ofMinutes(10), Duration.ofMinutes(70));
        saveQuestions(exam);
        saveAttempt(exam, false);

        mockMvc.perform(get("/api/student/exams/" + exam.getId()).header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("PUBLISHED")) // 공개됐고 시작 전
                .andExpect(jsonPath("$.result.startAt", endsWith("+09:00")))
                .andExpect(jsonPath("$.result.questionCount").value(2))
                .andExpect(jsonPath("$.result.attempt.status").value("WAITING"))
                .andExpect(jsonPath("$.result.attempt.submittedAt").isEmpty());
    }

    @Test
    void 입장하지_않은_시험이면_404() throws Exception {
        Exam exam = savePublishedExam("TEST01", Duration.ofMinutes(10), Duration.ofMinutes(70));

        mockMvc.perform(get("/api/student/exams/" + exam.getId()).header("Authorization", studentToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXAM_NOT_FOUND"));
    }

    @Test
    void 시작_전에_문항을_요청하면_403() throws Exception {
        Exam exam = savePublishedExam("TEST01", Duration.ofMinutes(10), Duration.ofMinutes(70));
        saveQuestions(exam);
        saveAttempt(exam, false);

        mockMvc.perform(get("/api/student/exams/" + exam.getId() + "/questions").header("Authorization", studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EXAM_NOT_STARTED"));
    }

    @Test
    void 시작하면_정답_없이_문항을_받고_응시_중이_된다() throws Exception {
        Exam exam = savePublishedExam("TEST01", Duration.ofMinutes(-1), Duration.ofMinutes(59)); // 1분 전 시작
        saveQuestions(exam);
        Attempt attempt = saveAttempt(exam, false);

        mockMvc.perform(get("/api/student/exams/" + exam.getId() + "/questions").header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(2))
                .andExpect(jsonPath("$.result[0].order").value(1))
                .andExpect(jsonPath("$.result[0].options[2]").value("√2"))
                .andExpect(jsonPath("$.result[0].answer").doesNotExist()); // 정답은 안 줌

        // 테스트 전체가 한 트랜잭션이라, 변경을 DB에 반영(flush)한 뒤 메모리를 비우고 다시 읽는다
        entityManager.flush();
        entityManager.clear();
        assertThat(attemptRepository.findById(attempt.getId()).orElseThrow().getStatus())
                .isEqualTo(AttemptStatus.IN_PROGRESS);
    }

    @Test
    void 답안을_저장하면_다시_불러올_수_있다() throws Exception {
        Exam exam = savePublishedExam("TEST01", Duration.ofMinutes(-1), Duration.ofMinutes(59));
        Attempt attempt = saveAttempt(exam, true);

        mockMvc.perform(get("/api/student/exams/" + exam.getId() + "/answers").header("Authorization", studentToken))
                .andExpect(jsonPath("$.result.version").value(0)) // 저장한 적 없으면 빈 답안
                .andExpect(jsonPath("$.result.answers").isEmpty());

        saveAnswers(exam, "{\"version\": 1, \"answers\": {\"11\": 2, \"12\": \"1/3\"}, \"saveFailCount\": 1}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.version").value(1))
                .andExpect(jsonPath("$.result.savedAt", endsWith("+09:00")));

        mockMvc.perform(get("/api/student/exams/" + exam.getId() + "/answers").header("Authorization", studentToken))
                .andExpect(jsonPath("$.result.version").value(1))
                .andExpect(jsonPath("$.result.answers.11").value(2))     // 객관식은 보기 인덱스
                .andExpect(jsonPath("$.result.answers.12").value("1/3")); // 단답형은 문자열

        Attempt saved = attemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(saved.getAnsweredCount()).isEqualTo(2);
        assertThat(saved.getSaveFailCount()).isEqualTo(1);
    }

    @Test
    void 늦게_도착한_예전_답안은_409이고_최신_답안이_남는다() throws Exception {
        Exam exam = savePublishedExam("TEST01", Duration.ofMinutes(-1), Duration.ofMinutes(59));
        saveAttempt(exam, true);
        saveAnswers(exam, "{\"version\": 2, \"answers\": {\"11\": 3}}").andExpect(status().isOk());

        saveAnswers(exam, "{\"version\": 1, \"answers\": {\"11\": 0}}") // 네트워크가 늦어 순서가 뒤바뀐 요청
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_VERSION"));

        mockMvc.perform(get("/api/student/exams/" + exam.getId() + "/answers").header("Authorization", studentToken))
                .andExpect(jsonPath("$.result.version").value(2))
                .andExpect(jsonPath("$.result.answers.11").value(3));
    }

    @Test
    void 제출하면_완료되고_다시_제출하면_409() throws Exception {
        Exam exam = savePublishedExam("TEST01", Duration.ofMinutes(-1), Duration.ofMinutes(59));
        saveAttempt(exam, true);

        submit(exam, "{\"version\": 1, \"answers\": {\"11\": 2}}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.submittedAt", endsWith("+09:00")));

        submit(exam, "{\"version\": 1, \"answers\": {\"11\": 2}}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_SUBMITTED"));

        mockMvc.perform(get("/api/student/exams/" + exam.getId()).header("Authorization", studentToken))
                .andExpect(jsonPath("$.result.attempt.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.result.attempt.submittedAt", endsWith("+09:00")));
    }

    @Test
    void 서버_답안이_더_최신이면_제출해도_서버_답안이_남는다() throws Exception {
        Exam exam = savePublishedExam("TEST01", Duration.ofMinutes(-1), Duration.ofMinutes(59));
        saveAttempt(exam, true);
        saveAnswers(exam, "{\"version\": 3, \"answers\": {\"11\": 3}}").andExpect(status().isOk());

        submit(exam, "{\"version\": 2, \"answers\": {\"11\": 0}}").andExpect(status().isOk());

        mockMvc.perform(get("/api/student/exams/" + exam.getId() + "/answers").header("Authorization", studentToken))
                .andExpect(jsonPath("$.result.version").value(3))
                .andExpect(jsonPath("$.result.answers.11").value(3));
    }

    @Test
    void 종료_후_30초까지는_제출되고_지나면_409() throws Exception {
        Exam justEnded = savePublishedExam("TEST01", Duration.ofMinutes(-60), Duration.ofSeconds(-10)); // 10초 전 종료
        saveAttempt(justEnded, true);
        submit(justEnded, "{\"version\": 1, \"answers\": {}}").andExpect(status().isOk());

        Exam longEnded = savePublishedExam("TEST02", Duration.ofMinutes(-60), Duration.ofMinutes(-1)); // 1분 전 종료
        saveAttempt(longEnded, true);
        submit(longEnded, "{\"version\": 1, \"answers\": {}}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXAM_CLOSED"));
    }

    // ---------- 도우미 ----------

    private ResultActions submit(Exam exam, String json) throws Exception {
        return mockMvc.perform(post("/api/student/exams/" + exam.getId() + "/submit").header("Authorization", studentToken)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions saveAnswers(Exam exam, String json) throws Exception {
        return mockMvc.perform(put("/api/student/exams/" + exam.getId() + "/answers").header("Authorization", studentToken)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions join(String code) throws Exception {
        return mockMvc.perform(post("/api/student/exams/join").header("Authorization", studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\": \"" + code + "\"}"));
    }

    // 공개된 시험. 시작·종료 시각은 지금 기준 (음수면 과거)
    private Exam savePublishedExam(String code, Duration startFromNow, Duration endFromNow) {
        Instant now = Instant.now();
        Exam exam = Exam.builder().title("중2 수학 1단원 평가")
                .startAt(now.plus(startFromNow)).endAt(now.plus(endFromNow)).createdBy(admin).build();
        exam.publish(code, now.minus(Duration.ofDays(1)));
        return examRepository.save(exam);
    }

    // 학생이 이미 입장한 상태 (started = true면 문항을 받아 응시 중)
    private Attempt saveAttempt(Exam exam, boolean started) {
        Attempt attempt = Attempt.builder().exam(exam).user(student).joinedAt(Instant.now()).build();
        if (started) {
            attempt.start(Instant.now());
        }
        attemptRepository.save(attempt);
        entityManager.flush();
        return attempt;
    }

    // 객관식 1 + 단답형 1
    private void saveQuestions(Exam exam) {
        questionRepository.saveAll(List.of(
                Question.builder().exam(exam).seq(1).type(QuestionType.MULTIPLE_CHOICE)
                        .content("다음 중 유리수가 아닌 것은?").options(List.of("0.5", "-3", "√2", "2/3")).answer("2").build(),
                Question.builder().exam(exam).seq(2).type(QuestionType.SHORT_ANSWER)
                        .content("0.333…을 기약분수로 나타내면?").answer("1/3").build()));
    }
}
