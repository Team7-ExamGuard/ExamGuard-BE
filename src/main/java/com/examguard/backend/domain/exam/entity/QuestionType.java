package com.examguard.backend.domain.exam.entity;

public enum QuestionType {
    MULTIPLE_CHOICE, // 객관식: 보기 4개, 답은 보기 인덱스 0~3
    SHORT_ANSWER     // 단답형: 답은 문자열
}
