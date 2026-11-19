package com.examguard.backend.global.exception;

import com.examguard.backend.global.payload.status.BaseStatus;
import com.examguard.backend.global.payload.status.ReasonDto;
import lombok.Getter;

/**
 * 서비스 로직에서 정해진 에러로 응답할 때 던지는 예외.
 * 도메인별 예외(AuthException, ExamException 등)는 이 클래스를 상속해서 만든다.
 */
@Getter
public class GeneralException extends RuntimeException {

    private final BaseStatus status;

    public GeneralException(BaseStatus status) {
        super(status.getReasonHttpStatus().getMessage()); // 로그에 메시지가 찍히도록
        this.status = status;
    }

    // 같은 에러 코드에 상황별 메시지를 줄 때 (예: VALIDATION_ERROR + "제목을 입력하세요.")
    public GeneralException(BaseStatus status, String message) {
        super(message);
        this.status = status;
    }

    public ReasonDto getErrorReasonHttpStatus() {
        return this.status.getReasonHttpStatus();
    }
}
