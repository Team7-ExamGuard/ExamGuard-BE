package com.examguard.backend.global.payload.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 에러 목록. code 값은 노션 API 명세서의 에러 코드와 같은 이름을 쓴다.
 * 도메인 에러는 해당 API를 만들 때 아래에 구역을 나눠 추가한다.
 */
@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseStatus {
    // 가장 일반적인 응답
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "지원하지 않는 요청 메서드입니다."),

    // 인증
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다."),

    // 시험
    EXAM_NOT_FOUND(HttpStatus.NOT_FOUND, "EXAM_NOT_FOUND", "시험을 찾을 수 없습니다."),
    EXAM_NOT_DRAFT(HttpStatus.CONFLICT, "EXAM_NOT_DRAFT", "공개된 시험은 수정할 수 없습니다."),
    EXAM_NOT_OPEN(HttpStatus.CONFLICT, "EXAM_NOT_OPEN", "종료할 수 없는 상태입니다."),
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
                .isSuccess(false)
                .build();
    }
}
