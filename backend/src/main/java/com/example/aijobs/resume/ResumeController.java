package com.example.aijobs.resume;

import com.example.aijobs.auth.AuthenticatedUser;
import com.example.aijobs.common.ApiResponse;
import com.example.aijobs.resume.dto.ResumeRequest;
import com.example.aijobs.resume.dto.ResumeResponse;
import com.example.aijobs.resume.dto.ResumeStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/resumes")
public class ResumeController {
    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @GetMapping
    public ApiResponse<List<ResumeResponse>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success("查询成功", resumeService.listOwned(user.id()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ResumeResponse> detail(@AuthenticationPrincipal AuthenticatedUser user,
                                              @PathVariable Long id) {
        return ApiResponse.success("查询成功", resumeService.getOwned(user.id(), id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ResumeResponse>> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                               @Valid @RequestBody ResumeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("简历草稿创建成功", resumeService.create(user.id(), request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<ResumeResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
                                              @PathVariable Long id,
                                              @Valid @RequestBody ResumeRequest request) {
        return ApiResponse.success("简历更新成功", resumeService.update(user.id(), id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<ResumeResponse> updateStatus(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody ResumeStatusRequest request) {
        return ApiResponse.success("简历状态更新成功", resumeService.updateStatus(user.id(), id, request.status()));
    }
}
