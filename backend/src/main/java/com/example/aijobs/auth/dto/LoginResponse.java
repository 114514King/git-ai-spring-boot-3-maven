package com.example.aijobs.auth.dto;

import java.time.Instant;

public record LoginResponse(String tokenType, String accessToken, Instant expiresAt) {
}
