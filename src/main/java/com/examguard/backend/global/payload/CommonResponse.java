package com.examguard.backend.global.payload;

import com.examguard.backend.global.payload.status.BaseStatus;
import com.examguard.backend.global.payload.status.SuccessStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class CommonResponse<T> {

    // 이름을 고정하지 않으면 Jackson이 boolean isXxx를 "success"로 바꿔서 내보냄
    @Getter(AccessLevel.NONE)
    @JsonProperty("isSuccess")
    private final boolean isSuccess;

    private final String code;

    private final String message;

    // 값이 없으면 응답에서 생략
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T result;

    // 응답 본문에는 안 나가고, ResponseStatusConverter가 HTTP 상태 코드로 사용
    @JsonIgnore
    private final HttpStatus httpStatus;

    // 성공시 응답 생성 (200 OK)
    public static <T> CommonResponse<T> onSuccess(T result) {
        return of(SuccessStatus.OK, result);
    }

    // 성공 상태를 지정해서 응답 생성 (예: 201 CREATED)
    public static <T> CommonResponse<T> of(BaseStatus status, T result) {
        return new CommonResponse<>(true,
                status.getReasonHttpStatus().getCode(),
                status.getReasonHttpStatus().getMessage(),
                result,
                status.getReasonHttpStatus().getHttpStatus());
    }

    // 전역 예외 처리에서 사용합니다.
    public static <T> CommonResponse<T> onFailure(String code, String message, T data) {
        return new CommonResponse<>(false, code, message, data, null);
    }
}
