package com.examguard.backend.domain.exam.entity;

import com.examguard.backend.domain.common.entity.BaseTimeEntity;
import com.examguard.backend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "exams")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exam extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(unique = true, length = 6)
    private String code; // publish 때 발급, DRAFT는 null

    private Instant startAt;

    private Instant endAt;

    private Instant publishedAt; // null이면 DRAFT

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Builder
    public Exam(String title, Instant startAt, Instant endAt, User createdBy) {
        this.title = title;
        this.startAt = startAt;
        this.endAt = endAt;
        this.createdBy = createdBy;
    }

    /**
     * 현재 시각 기준 시험 상태.
     * 시작 전에 조기 종료하면 endAt이 startAt보다 앞서므로 CLOSED를 PUBLISHED보다 먼저 판정한다.
     */
    public ExamStatus statusAt(Instant now) {
        if (publishedAt == null) {
            return ExamStatus.DRAFT;
        }
        if (!now.isBefore(endAt)) {
            return ExamStatus.CLOSED;
        }
        if (now.isBefore(startAt)) {
            return ExamStatus.PUBLISHED;
        }
        return ExamStatus.OPEN;
    }

    public boolean isPublished() {
        return publishedAt != null;
    }

    // 임시 저장 (DRAFT일 때만 호출)
    public void updateDraft(String title, Instant startAt, Instant endAt) {
        this.title = title;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    // 공개: 시험 코드 발급
    public void publish(String code, Instant now) {
        this.code = code;
        this.publishedAt = now;
    }

    // 조기 종료: 종료 시각을 지금으로 당김.
    // 시작 전에 종료(=시험 취소)하면 시작 시각도 지금으로 당겨서 startAt ≤ endAt을 유지
    public void close(Instant now) {
        this.endAt = now;
        if (startAt != null && startAt.isAfter(now)) {
            this.startAt = now;
        }
    }
}
