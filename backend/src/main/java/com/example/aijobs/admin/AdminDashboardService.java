package com.example.aijobs.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aijobs.admin.dto.AdminDashboardResponse;
import com.example.aijobs.admin.dto.AdminStatusCount;
import com.example.aijobs.application.entity.JobApplication;
import com.example.aijobs.application.mapper.JobApplicationMapper;
import com.example.aijobs.auth.entity.PlatformUser;
import com.example.aijobs.auth.entity.Role;
import com.example.aijobs.auth.entity.UserRole;
import com.example.aijobs.auth.mapper.PlatformUserMapper;
import com.example.aijobs.auth.mapper.RoleMapper;
import com.example.aijobs.auth.mapper.UserRoleMapper;
import com.example.aijobs.job.entity.JobPosting;
import com.example.aijobs.job.mapper.JobPostingMapper;
import com.example.aijobs.match.entity.AiMatchResult;
import com.example.aijobs.match.mapper.AiMatchResultMapper;
import com.example.aijobs.resume.entity.Resume;
import com.example.aijobs.resume.mapper.ResumeMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {
    private static final List<String> ROLE_CODES = List.of("STUDENT", "HR", "ADMIN");

    private final PlatformUserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final JobPostingMapper jobMapper;
    private final ResumeMapper resumeMapper;
    private final JobApplicationMapper applicationMapper;
    private final AiMatchResultMapper matchMapper;

    public AdminDashboardService(PlatformUserMapper userMapper,
                                 RoleMapper roleMapper,
                                 UserRoleMapper userRoleMapper,
                                 JobPostingMapper jobMapper,
                                 ResumeMapper resumeMapper,
                                 JobApplicationMapper applicationMapper,
                                 AiMatchResultMapper matchMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.jobMapper = jobMapper;
        this.resumeMapper = resumeMapper;
        this.applicationMapper = applicationMapper;
        this.matchMapper = matchMapper;
    }

    public AdminDashboardResponse dashboard() {
        List<PlatformUser> users = userMapper.selectList(null);
        List<JobPosting> jobs = jobMapper.selectList(null);
        List<Resume> resumes = resumeMapper.selectList(null);
        List<JobApplication> applications = applicationMapper.selectList(null);
        List<AiMatchResult> matches = matchMapper.selectList(null);
        Map<String, Long> roleCounts = countUsersByRole();

        long activeUsers = users.stream().filter(user -> "ACTIVE".equals(user.getStatus())).count();
        long publishedJobs = jobs.stream().filter(job -> "PUBLISHED".equals(job.getStatus())).count();
        long publishedResumes = resumes.stream().filter(resume -> "PUBLISHED".equals(resume.getStatus())).count();
        long activeApplications = applications.stream()
                .filter(application -> !"WITHDRAWN".equals(application.getStatus()))
                .count();

        return new AdminDashboardResponse(
                users.size(),
                activeUsers,
                roleCounts.getOrDefault("STUDENT", 0L),
                roleCounts.getOrDefault("HR", 0L),
                roleCounts.getOrDefault("ADMIN", 0L),
                jobs.size(),
                publishedJobs,
                resumes.size(),
                publishedResumes,
                applications.size(),
                activeApplications,
                matches.size(),
                averageScore(matches),
                ROLE_CODES.stream()
                        .map(code -> new AdminStatusCount(code, roleCounts.getOrDefault(code, 0L)))
                        .toList(),
                countStatus(jobs, JobPosting::getStatus),
                countStatus(resumes, Resume::getStatus),
                countStatus(applications, JobApplication::getStatus));
    }

    private Map<String, Long> countUsersByRole() {
        return roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(Role::getCode, role -> userRoleMapper.selectCount(
                        new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, role.getId()))));
    }

    private <T> List<AdminStatusCount> countStatus(List<T> rows, Function<T, String> statusGetter) {
        Map<String, Long> counts = rows.stream()
                .collect(Collectors.groupingBy(statusGetter, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new AdminStatusCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    private BigDecimal averageScore(List<AiMatchResult> matches) {
        if (matches.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }

        BigDecimal total = matches.stream()
                .map(AiMatchResult::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(matches.size()), 2, RoundingMode.HALF_UP);
    }
}
