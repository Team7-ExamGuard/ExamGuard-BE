package com.examguard.backend.global.security.jwt;

import com.examguard.backend.domain.user.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Access Token 발급·검증.
 * 토큰에 userId(subject)와 role을 넣어서, 요청마다 DB를 조회하지 않고 권한을 확인한다.
 * (학생 300명이 2~3초마다 답안을 저장하므로 요청당 DB 조회를 줄이려는 것)
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final Duration accessTokenValidity;

    public JwtTokenProvider(JwtProperties properties) {
        byte[] keyBytes = properties.secret() == null
                ? new byte[0]
                : properties.secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) { // HS256은 32바이트 이상 필요
            throw new IllegalStateException("JWT_SECRET이 없거나 32바이트보다 짧습니다. .env.example을 참고하세요.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValidity = properties.accessTokenValidity();
    }

    public String createAccessToken(Long userId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenValidity)))
                .signWith(key)
                .compact();
    }

    // 서명이 맞고 만료되지 않았으면 true
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) { // 만료(ExpiredJwtException)도 JwtException
            log.debug("유효하지 않은 JWT: {}", e.getMessage());
            return false;
        }
    }

    // 토큰 → 인증 정보. principal은 userId(Long), 권한은 ROLE_STUDENT / ROLE_ADMIN
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        Long userId = Long.valueOf(claims.getSubject());
        String role = claims.get(ROLE_CLAIM, String.class);
        return new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
