package com.example.aijobs.job.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record JobStatusRequest(
        @NotBlank @Pattern(regexp = "DRAFT|PUBLISHED|CLOSED") String status) {
}
