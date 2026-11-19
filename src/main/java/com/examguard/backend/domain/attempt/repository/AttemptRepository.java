package com.examguard.backend.domain.attempt.repository;

import com.examguard.backend.domain.attempt.entity.Attempt;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    // 학생의 해당 시험 응시 조회 (시험당 1건). 시험 정보도 쓰므로 한 번의 조회로 같이 가져온다
    @EntityGraph(attributePaths = "exam")
    Optional<Attempt> findByExamIdAndUserId(Long examId, Long userId);

    // 학생 입장: 응시 기록이 없을 때만 WAITING으로 만든다.
    // 입장 버튼을 두 번 누르는 등 같은 요청이 동시에 와도 (exam_id, user_id) 중복 에러 없이 한 건만 남는다
    @Modifying
    @Query(value = """
            insert into attempts (exam_id, user_id, status, joined_at)
            values (:examId, :userId, 'WAITING', :now)
            on conflict (exam_id, user_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("examId") Long examId, @Param("userId") Long userId, @Param("now") Instant now);

    // 관리자 시험 목록: 시험별 입장 인원
    @Query("select a.exam.id as examId, count(a) as joinedCount from Attempt a group by a.exam.id")
    List<JoinedCount> countJoinedByExam();

    interface JoinedCount {
        Long getExamId();

        Long getJoinedCount();
    }

    // 조기 종료: 응시 중인 학생을 마지막 저장 답안 그대로 제출 처리
    @Modifying
    @Query("""
            update Attempt a
               set a.status = com.examguard.backend.domain.attempt.entity.AttemptStatus.SUBMITTED,
                   a.submittedAt = :now
             where a.exam.id = :examId
               and a.status = com.examguard.backend.domain.attempt.entity.AttemptStatus.IN_PROGRESS
            """)
    int submitAllInProgress(@Param("examId") Long examId, @Param("now") Instant now);
}
