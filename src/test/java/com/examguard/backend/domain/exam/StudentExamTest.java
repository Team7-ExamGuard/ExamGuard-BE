package com.examguard.backend.domain.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
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
 * 학생 응시 흐름: 시험 코드로 입장
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 테스트에서 만든 데이터는 끝나면 지워짐
class StudentExamTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ExamRepository examRepository;
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

    // ---------- 도우미 ----------

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
}
