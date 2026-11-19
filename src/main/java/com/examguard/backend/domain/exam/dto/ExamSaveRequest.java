package com.examguard.backend.domain.exam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

// 시험 생성(POST)과 임시 저장(PUT)의 공통 본문. 문항 목록은 전체를 보내고, 배열 순서가 문항 순서
public record ExamSaveRequest(
        @Size(max = 200, message = "제목은 200자 이하로 입력해 주세요.")
        String title,

        OffsetDateTime startAt,

        OffsetDateTime endAt,

        List<@Valid QuestionSaveRequest> questions
) {
}
