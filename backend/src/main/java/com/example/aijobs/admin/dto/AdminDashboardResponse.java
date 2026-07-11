package com.example.aijobs.admin.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardResponse(
        long totalUsers,
        long activeUsers,
        long studentUsers,
        long hrUsers,
        long adminUsers,
        long totalJobs,
        long publishedJobs,
        long totalResumes,
        long publishedResumes,
        long totalApplications,
        long activeApplications,
        long totalMatches,
        BigDecimal averageMatchScore,
        List<AdminStatusCount> userRoleCounts,
        List<AdminStatusCount> jobStatusCounts,
        List<AdminStatusCount> resumeStatusCounts,
        List<AdminStatusCount> applicationStatusCounts,
        AdminAiOperationInsight aiOperationInsight) {
}
