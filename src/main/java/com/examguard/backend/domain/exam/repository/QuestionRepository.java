package com.examguard.backend.domain.exam.repository;

import com.examguard.backend.domain.exam.entity.Question;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    // 시험의 문항을 순서대로 조회
    List<Question> findByExamIdOrderBySeq(Long examId);
}
