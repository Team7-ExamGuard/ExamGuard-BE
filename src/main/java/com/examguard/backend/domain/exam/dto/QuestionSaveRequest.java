package com.examguard.backend.domain.exam.dto;

import com.examguard.backend.domain.exam.entity.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

// 시험 생성·임시 저장의 문항 하나. 임시 저장이라 type 외에는 비어 있어도 됨
public record QuestionSaveRequest(
        @Schema(description = "기존 문항 id (새 문항은 생략). 저장할 때 문항을 통째로 다시 만들기 때문에 참고용", example = "31")
        Long id,

        @NotNull(message = "문항 유형을 선택해 주세요.")
        QuestionType type,

        String content,

        @Schema(description = "객관식 보기 4개. 단답형은 null", example = "[\"0.5\", \"-3\", \"√2\", \"2/3\"]")
        List<String> options,

        @Schema(description = "객관식은 보기 인덱스 0~3(숫자), 단답형은 문자열", example = "2")
        Object answer
) {
}
