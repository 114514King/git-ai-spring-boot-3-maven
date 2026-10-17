package com.example.aijobs.job.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record JobRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 100) String companyName,
        @NotBlank @Size(max = 50) String city,
        @NotBlank @Pattern(regexp = "FULL_TIME|PART_TIME|INTERNSHIP") String employmentType,
        @DecimalMin("0") BigDecimal salaryMin,
        @DecimalMin("0") BigDecimal salaryMax,
        @NotBlank String description,
        @NotBlank String requirements) {
}
