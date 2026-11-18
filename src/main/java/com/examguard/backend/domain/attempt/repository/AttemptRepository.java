package com.examguard.backend.domain.attempt.repository;

import com.examguard.backend.domain.attempt.entity.Attempt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    // 학생의 해당 시험 응시 조회 (시험당 1건)
    Optional<Attempt> findByExamIdAndUserId(Long examId, Long userId);
}
