package com.example.aijobs.assistant;

import com.example.aijobs.auth.AuthenticatedUser;
import com.example.aijobs.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RecruitmentChatController {
    private final RecruitmentChatService chatService;

    public RecruitmentChatController(RecruitmentChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/student/assistant/chat")
    public ApiResponse<RecruitmentChatResponse> studentChat(@AuthenticationPrincipal AuthenticatedUser user,
                                                            @Valid @RequestBody RecruitmentChatRequest request) {
        return ApiResponse.success("对话生成成功", chatService.chat(user.id(), ChatRole.STUDENT, request));
    }

    @PostMapping("/hr/assistant/chat")
    public ApiResponse<RecruitmentChatResponse> hrChat(@AuthenticationPrincipal AuthenticatedUser user,
                                                       @Valid @RequestBody RecruitmentChatRequest request) {
        return ApiResponse.success("对话生成成功", chatService.chat(user.id(), ChatRole.HR, request));
    }
}
