package com.examguard.backend.global.security.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml의 jwt.* 설정을 담는다.
 *   jwt.secret                 서명 키 (JWT_SECRET 환경 변수)
 *   jwt.access-token-validity  토큰 유효 시간 (예: 3h)
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, Duration accessTokenValidity) {
}
