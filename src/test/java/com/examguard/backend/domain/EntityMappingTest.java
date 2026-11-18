package com.examguard.backend.domain;

import static org.assertj.core.api.Assertions.assertThat;

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
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 엔티티를 실제 DB에 저장했다가 다시 읽어서 jsonb 컬럼과 생성 시각 자동 입력이 동작하는지 확인한다.
 * (ddl-auto: validate는 컬럼 존재·타입만 검사하고 실제 저장은 해보지 않음)
 */
@SpringBootTest
@Transactional // 테스트가 끝나면 넣은 데이터를 되돌림
class EntityMappingTest {

    @Autowired UserRepository userRepository;
    @Autowired ExamRepository examRepository;
    @Autowired QuestionRepository questionRepository;
    @Autowired AttemptRepository attemptRepository;
    @Autowired EntityManager entityManager;

    @Test
    void 엔티티를_저장하고_다시_읽는다() {
        // 저장
        User admin = userRepository.save(User.builder()
                .loginId("test-admin").password("pw").name("관리자").role(Role.ADMIN).build());
        User student = userRepository.save(User.builder()
                .loginId("test-student").password("pw").name("학생").role(Role.STUDENT).build());
        Exam exam = examRepository.save(Exam.builder()
                .title("테스트 시험").createdBy(admin).build());
        questionRepository.save(Question.builder()
                .exam(exam).seq(1).type(QuestionType.MULTIPLE_CHOICE)
                .content("1 + 1은?").options(List.of("1", "2", "3", "4")).answer("1").build());
        attemptRepository.save(Attempt.builder()
                .exam(exam).user(student).joinedAt(Instant.now()).build());

        // DB에 실제로 쓰고, 메모리에 남은 엔티티를 비워서 아래 조회가 DB에서 다시 읽게 함
        entityManager.flush();
        entityManager.clear();

        // 다시 읽어서 확인
        Question question = questionRepository.findByExamIdOrderBySeq(exam.getId()).get(0);
        assertThat(question.getOptions()).containsExactly("1", "2", "3", "4"); // jsonb 배열

        Attempt attempt = attemptRepository.findByExamIdAndUserId(exam.getId(), student.getId()).orElseThrow();
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.WAITING);
        assertThat(attempt.getAnswers()).isEmpty(); // jsonb 객체 {}

        Exam savedExam = examRepository.findById(exam.getId()).orElseThrow();
        assertThat(savedExam.getCreatedAt()).isNotNull(); // 생성 시각 자동 입력
        assertThat(savedExam.getUpdatedAt()).isNotNull();
    }
}
