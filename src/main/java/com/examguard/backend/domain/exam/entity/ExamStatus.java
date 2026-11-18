package com.examguard.backend.domain.exam.entity;

/**
 * 시험 상태. DB에 저장하지 않고 Exam.statusAt(now)로 계산한다.
 */
public enum ExamStatus {
    DRAFT,     // 작성 중 (published_at 없음)
    PUBLISHED, // 공개됨, 시작 전
    OPEN,      // 진행 중
    CLOSED     // 종료 (end_at 지남 또는 조기 종료)
}
