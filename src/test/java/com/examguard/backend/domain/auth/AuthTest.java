package com.examguard.backend.domain.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examguard.backend.domain.user.entity.Role;
import com.examguard.backend.domain.user.entity.User;
import com.examguard.backend.domain.user.repository.UserRepository;
import com.examguard.backend.global.payload.CommonResponse;
import com.examguard.backend.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인과 경로별 권한(401·403)을 실제 Security 설정 그대로 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 테스트에서 만든 계정은 끝나면 지워짐
@Import(AuthTest.PingController.class)
class AuthTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @Test
    void 로그인_성공() throws Exception {
        userRepository.save(User.builder()
                .loginId("test-student").password(passwordEncoder.encode("pw1234")).name("테스트").role(Role.STUDENT).build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\": \"test-student\", \"password\": \"pw1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.role").value("STUDENT"))
                .andExpect(jsonPath("$.result.accessToken").isNotEmpty());
    }

    @Test
    void 비밀번호가_틀리면_401() throws Exception {
        userRepository.save(User.builder()
                .loginId("test-student").password(passwordEncoder.encode("pw1234")).name("테스트").role(Role.STUDENT).build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\": \"test-student\", \"password\": \"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void 토큰_없이_요청하면_401() throws Exception {
        mockMvc.perform(get("/api/student/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 학생이_관리자_API를_요청하면_403() throws Exception {
        String studentToken = jwtTokenProvider.createAccessToken(1L, Role.STUDENT);

        mockMvc.perform(get("/api/admin/ping").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void 관리자는_관리자_API를_요청할_수_있다() throws Exception {
        String adminToken = jwtTokenProvider.createAccessToken(1L, Role.ADMIN);

        mockMvc.perform(get("/api/admin/ping").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("pong"));
    }

    // 테스트에서만 쓰는 가짜 API
    @RestController
    static class PingController {

        @GetMapping("/api/student/ping")
        public CommonResponse<String> studentPing() {
            return CommonResponse.onSuccess("pong");
        }

        @GetMapping("/api/admin/ping")
        public CommonResponse<String> adminPing() {
            return CommonResponse.onSuccess("pong");
        }
    }
}
