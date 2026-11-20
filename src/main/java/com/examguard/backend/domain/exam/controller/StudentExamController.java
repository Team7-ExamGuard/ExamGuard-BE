package com.examguard.backend.domain.exam.controller;

import com.examguard.backend.domain.exam.dto.AnswersResponse;
import com.examguard.backend.domain.exam.dto.JoinRequest;
import com.examguard.backend.domain.exam.dto.JoinResponse;
import com.examguard.backend.domain.exam.dto.SaveAnswersRequest;
import com.examguard.backend.domain.exam.dto.SaveAnswersResponse;
import com.examguard.backend.domain.exam.dto.StudentExamResponse;
import com.examguard.backend.domain.exam.dto.StudentQuestionResponse;
import com.examguard.backend.domain.exam.service.StudentExamService;
import com.examguard.backend.global.payload.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 권한(STUDENT)은 SecurityConfig의 /api/student/** 규칙으로 확인
@Tag(name = "학생 - 시험 응시", description = "시험 코드 입장·시험 정보·문항 조회·답안 저장·복원")
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

    @Operation(summary = "문항 조회 (응시 시작)",
            description = "정답 제외. 처음 받으면 응시 중이 됨. 시작 전 403 EXAM_NOT_STARTED, 종료 409 EXAM_CLOSED, 제출 완료 409 ALREADY_SUBMITTED")
    @GetMapping("/{examId}/questions")
    public CommonResponse<List<StudentQuestionResponse>> getQuestions(@AuthenticationPrincipal Long userId,
                                                                      @PathVariable Long examId) {
        return CommonResponse.onSuccess(studentExamService.getQuestions(userId, examId));
    }

    @Operation(summary = "저장된 답안 조회 (복원)",
            description = "저장한 적 없으면 version 0, answers {}, savedAt null. 입장하지 않은 시험이면 404 EXAM_NOT_FOUND")
    @GetMapping("/{examId}/answers")
    public CommonResponse<AnswersResponse> getAnswers(@AuthenticationPrincipal Long userId,
                                                      @PathVariable Long examId) {
        return CommonResponse.onSuccess(studentExamService.getAnswers(userId, examId));
    }

    @Operation(summary = "답안 자동 저장",
            description = "answers 전체를 보냄. 저장된 version보다 큰 요청만 반영(아니면 409 STALE_VERSION). "
                    + "시작 전 403 EXAM_NOT_STARTED, 종료 시각 이후 409 EXAM_CLOSED, 제출 완료 409 ALREADY_SUBMITTED")
    @PutMapping("/{examId}/answers")
    public CommonResponse<SaveAnswersResponse> saveAnswers(@AuthenticationPrincipal Long userId,
                                                           @PathVariable Long examId,
                                                           @Valid @RequestBody SaveAnswersRequest request) {
        return CommonResponse.onSuccess(studentExamService.saveAnswers(userId, examId, request));
    }
}
