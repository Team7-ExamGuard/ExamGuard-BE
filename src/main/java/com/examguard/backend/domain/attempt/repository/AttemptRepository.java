package com.examguard.backend.domain.attempt.repository;

import com.examguard.backend.domain.attempt.entity.Attempt;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    // 최종 제출: 같은 응시에 저장·제출이 동시에 와도 하나씩 처리되도록 응시 행을 잠그고 읽는다 (SELECT … FOR UPDATE)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Attempt a where a.exam.id = :examId and a.user.id = :userId")
    Optional<Attempt> findForUpdate(@Param("examId") Long examId, @Param("userId") Long userId);

    // 답안 자동 저장: 저장된 version보다 큰 요청만 반영 (조건부 UPDATE 한 번)
    // 읽고 비교한 뒤 쓰면, 늦게 도착한 옛 요청이 그 사이에 최신 답안을 덮어쓸 수 있어서 DB가 비교하게 한다.
    // 바뀐 행이 0이면 더 최신 답안이 이미 있다는 뜻. 실행 후 영속성 컨텍스트를 비워 옛 값이 남지 않게 한다
    @Modifying(clearAutomatically = true)
    @Query("""
            update Attempt a
               set a.answers = :answers,
                   a.version = :version,
                   a.answeredCount = :answeredCount,
                   a.saveFailCount = a.saveFailCount + :saveFailCount,
                   a.lastSavedAt = :now
             where a.id = :attemptId
               and a.version < :version
               and a.status = com.examguard.backend.domain.attempt.entity.AttemptStatus.IN_PROGRESS
            """)
    int saveAnswersIfNewer(@Param("attemptId") Long attemptId,
                           @Param("version") int version,
                           @Param("answers") Map<String, Object> answers,
                           @Param("answeredCount") int answeredCount,
                           @Param("saveFailCount") int saveFailCount,
                           @Param("now") Instant now);

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
