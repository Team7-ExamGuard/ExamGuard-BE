package com.examguard.backend.domain.exam.dto;

import com.examguard.backend.domain.exam.entity.Question;
import com.examguard.backend.domain.exam.entity.QuestionType;
import java.util.List;

// 학생에게 주는 문항 (정답 없음). order는 1부터
public record StudentQuestionResponse(
        Long id,
        int order,
        QuestionType type,
        String content,
        List<String> options
) {

    public static StudentQuestionResponse from(Question question) {
        return new StudentQuestionResponse(
                question.getId(),
                question.getSeq(),
                question.getType(),
                question.getContent(),
                question.getOptions());
    }
}
