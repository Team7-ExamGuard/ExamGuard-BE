package com.examguard.backend.domain.attempt.entity;

import com.examguard.backend.domain.exam.entity.Exam;
import com.examguard.backend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 학생 한 명의 시험 응시 1건. (exam_id, user_id)는 유일하다.
 * 답안은 문항별 행이 아니라 answers(jsonb)에 통째로 저장하고, version은 응시당 하나다.
 */
@Entity
@Table(name = "attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attempt {

    // 종료 시각이 지나도 이만큼은 제출을 받아 줌 (네트워크 지연 대비)
    public static final Duration SUBMIT_GRACE = Duration.ofSeconds(30);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status;

    // jsonb 컬럼. { "문항id": 보기 인덱스 또는 문자열 }
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> answers = new HashMap<>();

    @Column(nullable = false)
    private int version; // 저장된 version보다 큰 요청만 반영

    @Column(nullable = false)
    private int answeredCount;

    @Column(nullable = false)
    private int saveFailCount;

    @Column(nullable = false)
    private Instant joinedAt;

    private Instant startedAt;

    private Instant lastSavedAt;

    private Instant submittedAt;

    // 시험 코드로 입장하면 WAITING 상태로 만들어진다
    @Builder
    public Attempt(Exam exam, User user, Instant joinedAt) {
        this.exam = exam;
        this.user = user;
        this.joinedAt = joinedAt;
        this.status = AttemptStatus.WAITING;
    }

    // 문항을 받아 응시 시작
    public void start(Instant now) {
        this.status = AttemptStatus.IN_PROGRESS;
        this.startedAt = now;
    }

    // 최종 제출
    public void submit(Instant now) {
        this.status = AttemptStatus.SUBMITTED;
        this.submittedAt = now;
    }

    // 답한 문항 수 (빈 값은 안 푼 것으로 봄). 관리자 현황 화면용
    public static int countAnswered(Map<String, Object> answers) {
        return (int) answers.values().stream()
                .filter(value -> value != null && !"".equals(value))
                .count();
    }

    // 종료 시각 + 유예가 지나도록 제출하지 않은 응시 중 학생은 마지막 저장 답안으로 제출 처리.
    // 시작하지 않은 WAITING 학생은 그대로 둔다. 조회할 때마다 확인하므로 별도 스케줄러가 없다
    public void submitIfExpired(Instant now) {
        if (status == AttemptStatus.IN_PROGRESS && !now.isBefore(exam.getEndAt().plus(SUBMIT_GRACE))) {
            submit(exam.getEndAt());
        }
    }
}
