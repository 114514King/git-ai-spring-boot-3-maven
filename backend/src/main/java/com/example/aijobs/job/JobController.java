package com.example.aijobs.job;

import com.example.aijobs.auth.AuthenticatedUser;
import com.example.aijobs.common.ApiResponse;
import com.example.aijobs.job.dto.JobRequest;
import com.example.aijobs.job.dto.JobResponse;
import com.example.aijobs.job.dto.JobStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class JobController {
    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/api/jobs")
    public ApiResponse<List<JobResponse>> list(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String city,
                                                @RequestParam(required = false) String employmentType) {
        return ApiResponse.success("查询成功", jobService.listPublished(keyword, city, employmentType));
    }

    @GetMapping("/api/jobs/{id}")
    public ApiResponse<JobResponse> detail(@PathVariable Long id) {
        return ApiResponse.success("查询成功", jobService.getPublished(id));
    }

    @GetMapping("/api/hr/jobs")
    public ApiResponse<List<JobResponse>> owned(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success("查询成功", jobService.listOwned(user.id()));
    }

    @PostMapping("/api/hr/jobs")
    public ResponseEntity<ApiResponse<JobResponse>> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                            @Valid @RequestBody JobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("岗位草稿创建成功", jobService.create(user.id(), request)));
    }

    @PutMapping("/api/hr/jobs/{id}")
    public ApiResponse<JobResponse> update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                           @Valid @RequestBody JobRequest request) {
        return ApiResponse.success("岗位更新成功", jobService.update(user.id(), id, request));
    }

    @PatchMapping("/api/hr/jobs/{id}/status")
    public ApiResponse<JobResponse> updateStatus(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @PathVariable Long id,
                                                 @Valid @RequestBody JobStatusRequest request) {
        return ApiResponse.success("岗位状态更新成功", jobService.updateStatus(user.id(), id, request.status()));
    }
}
