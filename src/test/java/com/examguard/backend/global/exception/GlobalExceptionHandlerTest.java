package com.examguard.backend.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examguard.backend.global.payload.CommonResponse;
import com.examguard.backend.global.payload.status.ErrorStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공통 응답·예외 처리가 기대한 JSON을 만드는지 확인한다.
 */
class GlobalExceptionHandlerTest {

    // 가짜 서버: 아래 TestController + 우리가 만든 GlobalExceptionHandler만 넣어서 만든다
    MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void 성공() throws Exception {
        mockMvc.perform(get("/test/ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("OK"));
    }

    @Test
    void GeneralException을_던지면_에러_응답() throws Exception {
        mockMvc.perform(get("/test/fail"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void 입력값_검증_실패() throws Exception {
        mockMvc.perform(post("/test/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.result.title").value("제목을 입력해 주세요."));
    }

    @Test
    void 없는_경로() throws Exception {
        mockMvc.perform(get("/test/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // 테스트에서만 쓰는 가짜 컨트롤러
    @RestController
    static class TestController {

        @GetMapping("/test/ok")
        public CommonResponse<String> ok() {
            return CommonResponse.onSuccess("hello");
        }

        @GetMapping("/test/fail")
        public CommonResponse<Void> fail() {
            throw new GeneralException(ErrorStatus.FORBIDDEN);
        }

        @PostMapping("/test/valid")
        public CommonResponse<Void> valid(@Valid @RequestBody TitleRequest request) {
            return CommonResponse.onSuccess(null);
        }
    }

    public record TitleRequest(@NotBlank(message = "제목을 입력해 주세요.") String title) {
    }
}
