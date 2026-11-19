package com.examguard.backend.global.exception;

import com.examguard.backend.global.payload.status.BaseStatus;

// 인증 관련 예외 (예: 로그인 실패)
public class AuthException extends GeneralException {

    public AuthException(BaseStatus status) {
        super(status);
    }
}
