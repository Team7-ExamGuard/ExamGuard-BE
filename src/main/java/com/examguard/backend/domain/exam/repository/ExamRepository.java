package com.examguard.backend.domain.exam.repository;

import com.examguard.backend.domain.exam.entity.Exam;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    // 학생 입장: 시험 코드로 시험 조회
    Optional<Exam> findByCode(String code);

    // 공개할 때 새로 만든 코드가 이미 쓰이는지 확인
    boolean existsByCode(String code);
}
