package com.examguard.backend.domain.attempt.repository;

import com.examguard.backend.domain.attempt.entity.Attempt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    // 학생의 해당 시험 응시 조회 (시험당 1건)
    Optional<Attempt> findByExamIdAndUserId(Long examId, Long userId);

    // 관리자 시험 목록: 시험별 입장 인원
    @Query("select a.exam.id as examId, count(a) as joinedCount from Attempt a group by a.exam.id")
    List<JoinedCount> countJoinedByExam();

    interface JoinedCount {
        Long getExamId();

        Long getJoinedCount();
    }
}
