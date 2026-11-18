package com.examguard.backend.domain.attempt.entity;

public enum AttemptStatus {
    WAITING,     // 코드로 입장, 시작 전
    IN_PROGRESS, // 문항을 받아 응시 중
    SUBMITTED    // 제출 완료
}
