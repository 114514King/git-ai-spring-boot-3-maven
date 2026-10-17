package com.example.aijobs.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ApplicationStatusRequest(
        @NotBlank @Pattern(regexp = "REVIEWING|INTERVIEW|OFFERED|REJECTED") String status) {
}
