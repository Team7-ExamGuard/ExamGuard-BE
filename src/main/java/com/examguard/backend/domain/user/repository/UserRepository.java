package com.examguard.backend.domain.user.repository;

import com.examguard.backend.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    // 로그인: 아이디로 계정 조회
    Optional<User> findByLoginId(String loginId);
}
