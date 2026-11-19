package com.examguard.backend.global.exception;

import com.examguard.backend.global.payload.status.BaseStatus;

// 시험 관련 예외 (예: 없는 시험, 공개된 시험 수정, 공개 검증 실패)
public class ExamException extends GeneralException {

    public ExamException(BaseStatus status) {
        super(status);
    }

    public ExamException(BaseStatus status, String message) {
        super(status, message);
    }
}
