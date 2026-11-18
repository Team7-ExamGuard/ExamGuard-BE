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
}
