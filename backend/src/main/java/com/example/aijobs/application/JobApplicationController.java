package com.example.aijobs.application;

import com.example.aijobs.application.dto.ApplicationRequest;
import com.example.aijobs.application.dto.ApplicationResponse;
import com.example.aijobs.application.dto.ApplicationFollowUpAdviceResponse;
import com.example.aijobs.application.dto.ApplicationStatusRequest;
import com.example.aijobs.application.dto.CandidateRecommendationResponse;
import com.example.aijobs.application.dto.InterviewKitResponse;
import com.example.aijobs.application.dto.StudentApplicationActionPlanResponse;
import com.example.aijobs.auth.AuthenticatedUser;
import com.example.aijobs.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class JobApplicationController {
    private final JobApplicationService applicationService;

    public JobApplicationController(JobApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/api/student/applications")
    public ApiResponse<List<ApplicationResponse>> listStudentApplications(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success("查询成功", applicationService.listStudentApplications(user.id()));
    }

    @PostMapping("/api/student/applications")
    public ResponseEntity<ApiResponse<ApplicationResponse>> submit(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("投递成功", applicationService.submit(user.id(), request)));
    }

    @PatchMapping("/api/student/applications/{id}/withdraw")
    public ApiResponse<ApplicationResponse> withdraw(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @PathVariable Long id) {
        return ApiResponse.success("投递已撤回", applicationService.withdraw(user.id(), id));
    }

    @PostMapping("/api/student/applications/{id}/action-plan")
    public ApiResponse<StudentApplicationActionPlanResponse> generateStudentActionPlan(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        return ApiResponse.success("行动计划生成成功", applicationService.generateStudentActionPlan(user.id(), id));
    }

    @GetMapping("/api/hr/applications")
    public ApiResponse<List<ApplicationResponse>> listHrApplications(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Long jobId) {
        return ApiResponse.success("查询成功", applicationService.listHrApplications(user.id(), jobId));
    }

    @GetMapping("/api/hr/applications/recommendations")
    public ApiResponse<List<CandidateRecommendationResponse>> recommendHrCandidates(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Long jobId) {
        return ApiResponse.success("推荐生成成功", applicationService.recommendHrCandidates(user.id(), jobId));
    }

    @PostMapping("/api/hr/applications/{id}/interview-kit")
    public ApiResponse<InterviewKitResponse> generateInterviewKit(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        return ApiResponse.success("面试题生成成功", applicationService.generateInterviewKit(user.id(), id));
    }

    @PostMapping("/api/hr/applications/{id}/follow-up-advice")
    public ApiResponse<ApplicationFollowUpAdviceResponse> generateFollowUpAdvice(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        return ApiResponse.success("跟进建议生成成功", applicationService.generateFollowUpAdvice(user.id(), id));
    }

    @PatchMapping("/api/hr/applications/{id}/status")
    public ApiResponse<ApplicationResponse> updateHrStatus(@AuthenticationPrincipal AuthenticatedUser user,
                                                           @PathVariable Long id,
                                                           @Valid @RequestBody ApplicationStatusRequest request) {
        return ApiResponse.success("投递状态更新成功",
                applicationService.updateHrStatus(user.id(), id, request.status()));
    }
}
