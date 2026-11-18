package com.examguard.backend.global.payload.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseStatus {
    // 일반적인 응답
    OK(HttpStatus.OK, "OK", "요청에 성공했습니다."),
    CREATED(HttpStatus.CREATED, "CREATED", "생성되었습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDto getReasonHttpStatus() {
        return ReasonDto.builder()
                .httpStatus(httpStatus)
                .message(message)
                .code(code)
                .isSuccess(true)
                .build();
    }
}
