package com.examguard.backend.domain.exam.dto;

import java.time.OffsetDateTime;

// 조기 종료 후 바뀐 종료 시각
public record CloseResponse(OffsetDateTime endAt) {
}
