package com.examguard.backend.domain.exam.dto;

import com.examguard.backend.domain.exam.entity.Question;
import com.examguard.backend.domain.exam.entity.QuestionType;
import java.util.List;

// 관리자 시험 상세의 문항 (정답 포함). order는 1부터
public record QuestionDetailResponse(
        Long id,
        int order,
        QuestionType type,
        String content,
        List<String> options,
        Object answer
) {

    public static QuestionDetailResponse from(Question question) {
        return new QuestionDetailResponse(
                question.getId(),
                question.getSeq(),
                question.getType(),
                question.getContent(),
                question.getOptions(),
                toApiAnswer(question));
    }

    // DB에는 문자열로 저장. 객관식은 명세대로 숫자(보기 인덱스)로 돌려준다
    private static Object toApiAnswer(Question question) {
        String answer = question.getAnswer();
        if (answer != null && question.getType() == QuestionType.MULTIPLE_CHOICE) {
            try {
                return Integer.parseInt(answer);
            } catch (NumberFormatException ignored) {
                // 임시 저장 중이라 숫자가 아닐 수 있음 → 그대로 돌려줌
            }
        }
        return answer;
    }
}
