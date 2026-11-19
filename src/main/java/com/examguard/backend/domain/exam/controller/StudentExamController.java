package com.examguard.backend.domain.exam.controller;

import com.examguard.backend.domain.exam.dto.JoinRequest;
import com.examguard.backend.domain.exam.dto.JoinResponse;
import com.examguard.backend.domain.exam.dto.StudentExamResponse;
import com.examguard.backend.domain.exam.service.StudentExamService;
import com.examguard.backend.global.payload.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 권한(STUDENT)은 SecurityConfig의 /api/student/** 규칙으로 확인
@Tag(name = "학생 - 시험 응시", description = "시험 코드 입장·시험 정보·문항 조회")
@RestController
@RequestMapping("/api/student/exams")
@RequiredArgsConstructor
public class StudentExamController {

    private final StudentExamService studentExamService;

    @Operation(summary = "시험 코드로 입장",
            description = "이미 입장했으면 같은 examId 반환. 없는 코드 404 EXAM_NOT_FOUND, 종료 409 EXAM_CLOSED, 제출 완료 409 ALREADY_SUBMITTED")
    @PostMapping("/join")
    public CommonResponse<JoinResponse> joinExam(@AuthenticationPrincipal Long userId,
                                                 @Valid @RequestBody JoinRequest request) {
        return CommonResponse.onSuccess(studentExamService.joinExam(userId, request.code()));
    }

    @Operation(summary = "시험 정보 (대기·응시·완료 화면 공통)",
            description = "내 응시 상태 포함. 입장하지 않은 시험이면 404 EXAM_NOT_FOUND")
    @GetMapping("/{examId}")
    public CommonResponse<StudentExamResponse> getExam(@AuthenticationPrincipal Long userId,
                                                       @PathVariable Long examId) {
        return CommonResponse.onSuccess(studentExamService.getExam(userId, examId));
    }
}
