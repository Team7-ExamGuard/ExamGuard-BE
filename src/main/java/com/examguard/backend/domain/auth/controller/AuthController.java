package com.examguard.backend.domain.auth.controller;

import com.examguard.backend.domain.auth.dto.LoginRequest;
import com.examguard.backend.domain.auth.dto.LoginResponse;
import com.examguard.backend.domain.auth.service.AuthService;
import com.examguard.backend.global.payload.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "로그인")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "로그인", description = "아이디·비밀번호로 로그인하고 Access Token을 발급합니다. 실패하면 401 INVALID_CREDENTIALS")
    @PostMapping("/login")
    public CommonResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return CommonResponse.onSuccess(authService.login(request));
    }
}
