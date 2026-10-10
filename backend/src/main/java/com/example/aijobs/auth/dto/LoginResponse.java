package com.example.aijobs.auth.dto;

import java.time.Instant;
import java.util.List;

public record LoginResponse(String tokenType, String accessToken, Instant expiresAt, List<String> roles) {
    public LoginResponse {
        roles = List.copyOf(roles);
    }
}
