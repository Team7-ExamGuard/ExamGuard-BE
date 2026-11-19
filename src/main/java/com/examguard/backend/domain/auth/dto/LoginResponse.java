package com.examguard.backend.domain.auth.dto;

import com.examguard.backend.domain.user.entity.Role;

// 노션 명세: { loginId, name, role, accessToken }
public record LoginResponse(
        String loginId,
        String name,
        Role role,
        String accessToken
) {
}
