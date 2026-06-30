package com.example.aijobs.admin;

import com.example.aijobs.admin.dto.AdminDashboardResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTests {
    @Mock private PlatformUserMapper userMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private UserRoleMapper userRoleMapper;
    @Mock private JobPostingMapper jobMapper;
    @Mock private ResumeMapper resumeMapper;
    @Mock private JobApplicationMapper applicationMapper;
    @Mock private AiMatchResultMapper matchMapper;
    private AdminDashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new AdminDashboardService(userMapper, roleMapper, userRoleMapper,
                jobMapper, resumeMapper, applicationMapper, matchMapper);
    }

    @Test
    void dashboardAggregatesPlatformCounts() {
        when(userMapper.selectList(null)).thenReturn(List.of(user("ACTIVE"), user("ACTIVE"), user("DISABLED")));
        when(roleMapper.selectList(null)).thenReturn(List.of(role(1L, "STUDENT"), role(2L, "HR"), role(3L, "ADMIN")));
        when(userRoleMapper.selectCount(any())).thenReturn(1L, 1L, 1L);
        when(jobMapper.selectList(null)).thenReturn(List.of(job("PUBLISHED"), job("DRAFT")));
        when(resumeMapper.selectList(null)).thenReturn(List.of(resume("PUBLISHED"), resume("DRAFT")));
        when(applicationMapper.selectList(null)).thenReturn(List.of(application("SUBMITTED"), application("WITHDRAWN")));
        when(matchMapper.selectList(null)).thenReturn(List.of(match("86.50"), match("93.50")));

        AdminDashboardResponse response = dashboardService.dashboard();

        assertEquals(3, response.totalUsers());
        assertEquals(2, response.activeUsers());
        assertEquals(1, response.studentUsers());
        assertEquals(2, response.totalJobs());
        assertEquals(1, response.publishedJobs());
        assertEquals(1, response.activeApplications());
        assertEquals(new BigDecimal("90.00"), response.averageMatchScore());
        assertEquals(2, response.jobStatusCounts().size());
    }

    private PlatformUser user(String status) {
        PlatformUser user = new PlatformUser();
        user.setStatus(status);
        return user;
    }

    private Role role(Long id, String code) {
        Role role = new Role();
        role.setId(id);
        role.setCode(code);
        return role;
    }

    private JobPosting job(String status) {
        JobPosting job = new JobPosting();
        job.setStatus(status);
        return job;
    }

    private Resume resume(String status) {
        Resume resume = new Resume();
        resume.setStatus(status);
        return resume;
    }

    private JobApplication application(String status) {
        JobApplication application = new JobApplication();
        application.setStatus(status);
        return application;
    }

    private AiMatchResult match(String score) {
        AiMatchResult match = new AiMatchResult();
        match.setScore(new BigDecimal(score));
        return match;
    }
}
