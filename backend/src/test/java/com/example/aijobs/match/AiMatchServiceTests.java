package com.example.aijobs.match;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.aijobs.application.entity.JobApplication;
import com.example.aijobs.application.mapper.JobApplicationMapper;
import com.example.aijobs.common.BusinessException;
import com.example.aijobs.job.entity.JobPosting;
import com.example.aijobs.job.mapper.JobPostingMapper;
import com.example.aijobs.match.dto.MatchRequest;
import com.example.aijobs.match.dto.MatchResponse;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiMatchServiceTests {
    @Mock private AiMatchResultMapper matchMapper;
    @Mock private ResumeMapper resumeMapper;
    @Mock private JobPostingMapper jobMapper;
    @Mock private JobApplicationMapper applicationMapper;
    private AiMatchService matchService;

    @BeforeEach
    void setUp() {
        matchService = new AiMatchService(matchMapper, resumeMapper, jobMapper, applicationMapper);
    }

    @Test
    void studentCanGenerateMatchForOwnedPublishedResumeAndPublishedJob() {
        when(resumeMapper.selectById(5L)).thenReturn(publishedResume(42L));
        when(jobMapper.selectById(9L)).thenReturn(publishedJob(7L));
        when(matchMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            AiMatchResult result = invocation.getArgument(0);
            result.setId(21L);
            return 1;
        }).when(matchMapper).insert(any(AiMatchResult.class));

        MatchResponse response = matchService.matchForStudent(42L, new MatchRequest(5L, 9L));

        assertEquals(21L, response.id());
        assertEquals(5L, response.resumeId());
        assertEquals(9L, response.jobId());
        assertEquals(new BigDecimal("33.33"), response.score());
        assertEquals("local-keyword-match-v2", response.modelName());
        assertTrue(response.analysis().contains("java"));
        assertTrue(response.strengthSummary().contains("java"));
        assertTrue(response.gapSummary().contains("开发工程师"));
        assertTrue(response.actionSuggestions().contains("缺口关键词"));
    }

    @Test
    void studentCannotGenerateMatchForAnotherStudentResume() {
        when(resumeMapper.selectById(5L)).thenReturn(publishedResume(42L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> matchService.matchForStudent(99L, new MatchRequest(5L, 9L)));

        assertEquals(403, exception.getStatus().value());
        verify(matchMapper, never()).insert(any(AiMatchResult.class));
    }

    @Test
    void existingStudentMatchIsUpdated() {
        AiMatchResult existing = new AiMatchResult();
        existing.setId(21L);
        existing.setResumeId(5L);
        existing.setJobId(9L);
        existing.setScore(new BigDecimal("1.00"));
        when(resumeMapper.selectById(5L)).thenReturn(publishedResume(42L));
        when(jobMapper.selectById(9L)).thenReturn(publishedJob(7L));
        when(matchMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        MatchResponse response = matchService.matchForStudent(42L, new MatchRequest(5L, 9L));

        assertEquals(21L, response.id());
        assertEquals(new BigDecimal("33.33"), response.score());
        assertNotNull(response.strengthSummary());
        assertNotNull(response.gapSummary());
        assertNotNull(response.actionSuggestions());
        verify(matchMapper).updateById(existing);
        verify(matchMapper, never()).insert(any(AiMatchResult.class));
    }

    @Test
    void hrCanGenerateMatchForApplicationOnOwnedPublishedJob() {
        when(jobMapper.selectById(9L)).thenReturn(publishedJob(7L));
        when(resumeMapper.selectById(5L)).thenReturn(publishedResume(42L));
        when(applicationMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(matchMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        MatchResponse response = matchService.matchForHr(7L, new MatchRequest(5L, 9L));

        assertEquals(9L, response.jobId());
        verify(matchMapper).insert(any(AiMatchResult.class));
    }

    @Test
    void hrCannotGenerateMatchWithoutApplication() {
        when(jobMapper.selectById(9L)).thenReturn(publishedJob(7L));
        when(resumeMapper.selectById(5L)).thenReturn(publishedResume(42L));
        when(applicationMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> matchService.matchForHr(7L, new MatchRequest(5L, 9L)));

        assertEquals(403, exception.getStatus().value());
        verify(matchMapper, never()).insert(any(AiMatchResult.class));
    }

    @Test
    void anotherHrCannotGenerateMatchForJob() {
        when(jobMapper.selectById(9L)).thenReturn(publishedJob(7L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> matchService.matchForHr(99L, new MatchRequest(5L, 9L)));

        assertEquals(403, exception.getStatus().value());
        verify(resumeMapper, never()).selectById(5L);
    }

    private Resume publishedResume(Long studentId) {
        Resume resume = new Resume();
        resume.setId(5L);
        resume.setStudentId(studentId);
        resume.setTitle("Java 后端简历");
        resume.setSkills("Java Spring Boot MySQL");
        resume.setProjectExperience("招聘平台后端开发");
        resume.setStatus("PUBLISHED");
        return resume;
    }

    private JobPosting publishedJob(Long hrId) {
        JobPosting job = new JobPosting();
        job.setId(9L);
        job.setHrId(hrId);
        job.setTitle("Java 开发工程师");
        job.setDescription("负责 Java 后端服务");
        job.setRequirements("Spring Boot 经验");
        job.setEmploymentType("FULL_TIME");
        job.setStatus("PUBLISHED");
        return job;
    }
}
