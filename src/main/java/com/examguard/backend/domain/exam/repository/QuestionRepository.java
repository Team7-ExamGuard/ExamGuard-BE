package com.examguard.backend.domain.exam.repository;

import com.examguard.backend.domain.exam.entity.Question;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    // 시험의 문항을 순서대로 조회
    List<Question> findByExamIdOrderBySeq(Long examId);

    // 임시 저장 때 기존 문항을 한 번에 지움 (바로 DELETE 실행)
    // JPA가 INSERT를 DELETE보다 먼저 보내면 (exam_id, seq) 중복 에러가 나므로, 지우기를 먼저 확정한다
    @Modifying
    @Query("delete from Question q where q.exam.id = :examId")
    void deleteAllByExamId(@Param("examId") Long examId);
}
