package com.examguard.backend.domain.exam.dto;

// 공개하면 발급되는 시험 코드 (예: "A1B2C3")
public record PublishResponse(String code) {
}
