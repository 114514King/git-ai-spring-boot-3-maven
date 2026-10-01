package com.example.aijobs.externaljobs;

import java.time.Instant;
import java.util.List;

public record ExternalJobsPage(String source, String sourceName, String sourceUrl,
                               List<ExternalJobResponse> jobs, int page, int pageSize,
                               int total, int totalPages, Instant lastSyncedAt,
                               boolean stale, String warning, String coverage) {
    public ExternalJobsPage {
        jobs = List.copyOf(jobs);
    }
}
