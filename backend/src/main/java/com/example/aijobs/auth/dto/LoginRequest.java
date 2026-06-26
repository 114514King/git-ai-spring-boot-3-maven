package com.example.aijobs.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 100) String account,
        @NotBlank @Size(max = 72) String password
) {
}
