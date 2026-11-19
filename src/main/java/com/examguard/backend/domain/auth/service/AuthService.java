package com.examguard.backend.domain.auth.service;

import com.examguard.backend.domain.auth.dto.LoginRequest;
import com.examguard.backend.domain.auth.dto.LoginResponse;
import com.examguard.backend.domain.user.entity.User;
import com.examguard.backend.domain.user.repository.UserRepository;
import com.examguard.backend.global.exception.AuthException;
import com.examguard.backend.global.payload.status.ErrorStatus;
import com.examguard.backend.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse login(LoginRequest request) {
        // 아이디 없음과 비밀번호 틀림을 같은 에러로 응답 (어느 쪽이 틀렸는지 알려주지 않음)
        User user = userRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new AuthException(ErrorStatus.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new AuthException(ErrorStatus.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        return new LoginResponse(user.getLoginId(), user.getName(), user.getRole(), accessToken);
    }
}
