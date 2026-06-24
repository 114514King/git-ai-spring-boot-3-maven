package com.example.aijobs.match;

import com.example.aijobs.auth.AuthenticatedUser;
import com.example.aijobs.common.ApiResponse;
import com.example.aijobs.match.dto.MatchRequest;
import com.example.aijobs.match.dto.MatchResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AiMatchController {
    private final AiMatchService matchService;

    public AiMatchController(AiMatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/api/student/matches")
    public ApiResponse<List<MatchResponse>> listStudentMatches(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Long resumeId) {
        return ApiResponse.success("查询成功", matchService.listStudentMatches(user.id(), resumeId));
    }

    @PostMapping("/api/student/matches")
    public ApiResponse<MatchResponse> matchForStudent(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @Valid @RequestBody MatchRequest request) {
        return ApiResponse.success("匹配完成", matchService.matchForStudent(user.id(), request));
    }

    @GetMapping("/api/hr/matches")
    public ApiResponse<List<MatchResponse>> listHrMatches(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Long jobId) {
        return ApiResponse.success("查询成功", matchService.listHrMatches(user.id(), jobId));
    }

    @PostMapping("/api/hr/matches")
    public ApiResponse<MatchResponse> matchForHr(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @Valid @RequestBody MatchRequest request) {
        return ApiResponse.success("匹配完成", matchService.matchForHr(user.id(), request));
    }
}
