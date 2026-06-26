package com.example.aijobs.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Email @Size(max = 100) String email,
        @Pattern(regexp = "^$|^[0-9+ -]{6,20}$", message = "手机号格式不正确") String phone,
        @Size(max = 50) String realName,
        @NotBlank @Pattern(regexp = "STUDENT|HR", message = "只能注册 STUDENT 或 HR") String role
) {
}
