package com.examguard.backend.domain.exam.entity;

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
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(nullable = false)
    private int seq; // 문항 순서

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType type;

    @Column(nullable = false)
    private String content;

    // jsonb 컬럼. 객관식 보기 4개, 단답형은 null
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> options;

    private String answer; // 객관식은 "0"~"3", 단답형은 문자열. DRAFT에서는 null 가능

    @Builder
    public Question(Exam exam, int seq, QuestionType type, String content, List<String> options, String answer) {
        this.exam = exam;
        this.seq = seq;
        this.type = type;
        this.content = content;
        this.options = options;
        this.answer = answer;
    }
}
