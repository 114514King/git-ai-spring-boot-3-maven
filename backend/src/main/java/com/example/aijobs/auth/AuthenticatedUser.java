package com.example.aijobs.auth;

import java.util.List;

public record AuthenticatedUser(Long id, String username, List<String> roles) {
}
