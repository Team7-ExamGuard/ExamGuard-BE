package com.examguard.backend.domain.exam.dto;

import java.time.OffsetDateTime;

public record SubmitResponse(
        OffsetDateTime submittedAt
) {
}
