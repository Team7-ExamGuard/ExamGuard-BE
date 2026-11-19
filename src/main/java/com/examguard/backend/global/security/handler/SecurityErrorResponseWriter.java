package com.examguard.backend.global.security.handler;

import com.examguard.backend.global.payload.CommonResponse;
import com.examguard.backend.global.payload.status.ErrorStatus;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Security 필터에서 막힌 요청(401·403)을 공통 응답 형식 JSON으로 쓴다.
 * 필터에서 난 에러는 컨트롤러 밖이라 GlobalExceptionHandler까지 가지 않으므로 여기서 직접 응답한다.
 * ObjectMapper는 Spring Boot 4 기본인 Jackson 3(tools.jackson) 것을 쓴다.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, ErrorStatus status) throws IOException {
        response.setStatus(status.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(),
                CommonResponse.onFailure(status.getCode(), status.getMessage(), null));
    }
}
