package com.examguard.backend.domain.exam.controller;

import com.examguard.backend.domain.exam.dto.CloseResponse;
import com.examguard.backend.domain.exam.dto.ExamDetailResponse;
import com.examguard.backend.domain.exam.dto.ExamIdResponse;
import com.examguard.backend.domain.exam.dto.ExamSaveRequest;
import com.examguard.backend.domain.exam.dto.ExamSummaryResponse;
import com.examguard.backend.domain.exam.dto.PublishResponse;
import com.examguard.backend.domain.exam.service.AdminExamService;
import com.examguard.backend.global.payload.CommonResponse;
import com.examguard.backend.global.payload.status.SuccessStatus;
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

// 권한(ADMIN)은 SecurityConfig의 /api/admin/** 규칙으로 확인
@Tag(name = "관리자 - 시험 출제", description = "시험 생성·임시 저장·공개·조기 종료")
@RestController
@RequestMapping("/api/admin/exams")
@RequiredArgsConstructor
public class AdminExamController {

    private final AdminExamService adminExamService;

    @Operation(summary = "시험 목록", description = "최근에 만든 시험부터. DRAFT는 code가 null")
    @GetMapping
    public CommonResponse<List<ExamSummaryResponse>> getExams() {
        return CommonResponse.onSuccess(adminExamService.getExams());
    }

    @Operation(summary = "시험 상세 (정답 포함)")
    @GetMapping("/{examId}")
    public CommonResponse<ExamDetailResponse> getExam(@PathVariable Long examId) {
        return CommonResponse.onSuccess(adminExamService.getExam(examId));
    }

    @Operation(summary = "시험 생성 (DRAFT)", description = "임시 저장과 같은 본문. 201 CREATED")
    @PostMapping
    public CommonResponse<ExamIdResponse> createExam(@AuthenticationPrincipal Long adminId,
                                                     @Valid @RequestBody ExamSaveRequest request) {
        return CommonResponse.of(SuccessStatus.CREATED, adminExamService.createExam(adminId, request));
    }

    @Operation(summary = "시험 임시 저장 (DRAFT만)", description = "문항 목록 전체를 보냄. 공개된 시험이면 409 EXAM_NOT_DRAFT")
    @PutMapping("/{examId}")
    public CommonResponse<ExamIdResponse> updateExam(@PathVariable Long examId,
                                                     @Valid @RequestBody ExamSaveRequest request) {
        return CommonResponse.onSuccess(adminExamService.updateExam(examId, request));
    }

    @Operation(summary = "시험 공개 & 코드 발급", description = "검증에 걸리면 400 VALIDATION_ERROR(첫 번째 이유가 message), 이미 공개됐으면 409 EXAM_NOT_DRAFT")
    @PostMapping("/{examId}/publish")
    public CommonResponse<PublishResponse> publishExam(@PathVariable Long examId) {
        return CommonResponse.onSuccess(adminExamService.publishExam(examId));
    }

    @Operation(summary = "시험 조기 종료", description = "시작 전이면 취소, 진행 중이면 응시 중 학생 제출 처리. DRAFT·CLOSED면 409 EXAM_NOT_OPEN")
    @PostMapping("/{examId}/close")
    public CommonResponse<CloseResponse> closeExam(@PathVariable Long examId) {
        return CommonResponse.onSuccess(adminExamService.closeExam(examId));
    }
}
