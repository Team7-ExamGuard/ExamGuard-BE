package com.examguard.backend.global.exception;

import com.examguard.backend.global.payload.CommonResponse;
import com.examguard.backend.global.payload.status.ErrorStatus;
import com.examguard.backend.global.payload.status.ReasonDto;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 컨트롤러에서 올라온 예외를 공통 응답 형식으로 바꾼다.
 * 인증·인가 실패(401·403)는 Security 필터에서 나므로 여기까지 오지 않고 따로 처리한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // 서비스에서 던진 GeneralException (도메인 예외 포함)
    @ExceptionHandler(value = GeneralException.class)
    public ResponseEntity<Object> handleGeneral(GeneralException generalException, WebRequest request) {
        ReasonDto errorReasonHttpStatus = generalException.getErrorReasonHttpStatus();

        CommonResponse<Object> body = CommonResponse.onFailure(errorReasonHttpStatus.getCode(),
                errorReasonHttpStatus.getMessage(),
                null);

        return super.handleExceptionInternal(generalException, body, HttpHeaders.EMPTY,
                errorReasonHttpStatus.getHttpStatus(), request);
    }

    // @Valid 실패: result에 필드별 메시지 (한 필드에 여러 개면 ", "로 이어 붙임)
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        e.getBindingResult().getFieldErrors()
                .forEach(fieldError -> {
                    String fieldName = fieldError.getField();
                    String errorMessage = Optional.ofNullable(fieldError.getDefaultMessage()).orElse("");
                    errors.merge(fieldName, errorMessage,
                            (existingErrorMessage, newErrorMessage) -> existingErrorMessage + ", " + newErrorMessage);
                });

        CommonResponse<Object> body = CommonResponse.onFailure(ErrorStatus.VALIDATION_ERROR.getCode(),
                ErrorStatus.VALIDATION_ERROR.getMessage(),
                errors);

        return super.handleExceptionInternal(e, body, HttpHeaders.EMPTY,
                ErrorStatus.VALIDATION_ERROR.getHttpStatus(), request);
    }

    // 그 밖의 모든 예외 → 500
    @ExceptionHandler
    public ResponseEntity<Object> handleOthers(Exception e, WebRequest request) {
        log.error("처리하지 못한 예외", e);

        CommonResponse<Object> body = CommonResponse.onFailure(ErrorStatus.INTERNAL_ERROR.getCode(),
                ErrorStatus.INTERNAL_ERROR.getMessage(),
                null);

        return super.handleExceptionInternal(e, body, HttpHeaders.EMPTY,
                ErrorStatus.INTERNAL_ERROR.getHttpStatus(), request);
    }

    // 부모 클래스가 처리하는 Spring MVC 예외(깨진 JSON, 없는 경로, 지원하지 않는 메서드 등)는
    // 본문 없이 이 메서드로 들어온다. 이때 본문을 공통 응답 형식으로 채운다.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception e,
                                                             Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        if (!(body instanceof CommonResponse)) {
            ErrorStatus errorStatus = toErrorStatus(statusCode);
            body = CommonResponse.onFailure(errorStatus.getCode(), errorStatus.getMessage(), null);
        }
        return super.handleExceptionInternal(e, body, headers, statusCode, request);
    }

    private ErrorStatus toErrorStatus(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 401 -> ErrorStatus.UNAUTHORIZED;
            case 403 -> ErrorStatus.FORBIDDEN;
            case 404 -> ErrorStatus.NOT_FOUND;
            case 405 -> ErrorStatus.METHOD_NOT_ALLOWED;
            default -> statusCode.is4xxClientError() ? ErrorStatus.VALIDATION_ERROR : ErrorStatus.INTERNAL_ERROR;
        };
    }
}
