package com.example.aijobs.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTests {

    @Test
    void issuedTokenCanBeParsed() {
        JwtService service = new JwtService("test-secret-key-with-at-least-thirty-two-bytes", Duration.ofHours(2));

        JwtService.Token token = service.issue(42L, "student1");
        Claims claims = service.parse(token.value());

        assertEquals("42", claims.getSubject());
        assertEquals("student1", claims.get("username", String.class));
        assertTrue(token.expiresAt().isAfter(claims.getIssuedAt().toInstant()));
    }
}
