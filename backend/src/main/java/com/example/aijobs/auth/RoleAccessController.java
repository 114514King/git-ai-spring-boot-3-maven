package com.example.aijobs.auth;

import com.example.aijobs.common.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access")
public class RoleAccessController {

    @GetMapping("/student")
    public ApiResponse<AuthenticatedUser> student(@AuthenticationPrincipal AuthenticatedUser user) {
        return granted(user);
    }

    @GetMapping("/hr")
    public ApiResponse<AuthenticatedUser> hr(@AuthenticationPrincipal AuthenticatedUser user) {
        return granted(user);
    }

    @GetMapping("/admin")
    public ApiResponse<AuthenticatedUser> admin(@AuthenticationPrincipal AuthenticatedUser user) {
        return granted(user);
    }

    private ApiResponse<AuthenticatedUser> granted(AuthenticatedUser user) {
        return ApiResponse.success("权限验证通过", user);
    }
}
