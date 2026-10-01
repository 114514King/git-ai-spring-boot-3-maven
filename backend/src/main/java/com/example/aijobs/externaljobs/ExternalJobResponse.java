package com.example.aijobs.externaljobs;

import java.time.Instant;
import java.util.List;

public record ExternalJobResponse(String id, String title, String companyName, String location,
                                  String employmentType, String salaryText, Instant publishedAt,
                                  String description, List<String> tags, String sourceUrl) {
    public ExternalJobResponse {
        tags = List.copyOf(tags);
    }
}
