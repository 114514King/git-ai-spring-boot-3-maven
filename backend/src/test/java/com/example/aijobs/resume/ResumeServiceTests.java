package com.example.aijobs.resume;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.aijobs.common.BusinessException;
import com.example.aijobs.job.entity.JobPosting;
import com.example.aijobs.job.mapper.JobPostingMapper;
import com.example.aijobs.resume.dto.ResumeOptimizationRequest;
import com.example.aijobs.resume.dto.ResumeOptimizationResponse;
import com.example.aijobs.resume.dto.ResumeRequest;
import com.example.aijobs.resume.dto.ResumeResponse;
import com.example.aijobs.resume.entity.Resume;
import com.example.aijobs.resume.mapper.ResumeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTests {
    @Mock private ResumeMapper resumeMapper;
    @Mock private JobPostingMapper jobMapper;
    private ResumeService resumeService;

    @BeforeEach
    void setUp() {
        resumeService = new ResumeService(resumeMapper, jobMapper);
    }

    @Test
    void createStoresOwnedDraft() {
        doAnswer(invocation -> {
            Resume resume = invocation.getArgument(0);
            resume.setId(7L);
            return 1;
        }).when(resumeMapper).insert(any(Resume.class));

        ResumeResponse response = resumeService.create(42L, request());

        assertEquals(7L, response.id());
        assertEquals(42L, response.studentId());
        assertEquals("DRAFT", response.status());
        verify(resumeMapper).insert(any(Resume.class));
    }

    @Test
    void ownedListMapsResults() {
        when(resumeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(ownedResume(42L)));

        List<ResumeResponse> response = resumeService.listOwned(42L);

        assertEquals(1, response.size());
        assertEquals("Java 后端简历", response.getFirst().title());
    }

    @Test
    void anotherStudentCannotReadResume() {
        when(resumeMapper.selectById(7L)).thenReturn(ownedResume(42L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> resumeService.getOwned(99L, 7L));

        assertEquals(403, exception.getStatus().value());
    }

    @Test
    void ownerCanPublishResume() {
        when(resumeMapper.selectById(7L)).thenReturn(ownedResume(42L));

        ResumeResponse response = resumeService.updateStatus(42L, 7L, "PUBLISHED");

        assertEquals("PUBLISHED", response.status());
        verify(resumeMapper).updateById(any(Resume.class));
    }

    @Test
    void ownerCanGenerateOptimizationForPublishedJob() {
        when(resumeMapper.selectById(7L)).thenReturn(ownedResume(42L));
        when(jobMapper.selectById(9L)).thenReturn(publishedJob());

        ResumeOptimizationResponse response = resumeService.optimizeForJob(
                42L, 7L, new ResumeOptimizationRequest(9L));

        assertEquals(7L, response.resumeId());
        assertEquals(9L, response.jobId());
        assertEquals("local-resume-optimizer-v1", response.modelName());
        assertTrue(response.matchedKeywords().contains("java"));
        assertTrue(response.missingKeywords().contains("spring"));
        assertEquals(4, response.contentSuggestions().size());
        assertEquals(3, response.actionPlan().size());
    }

    @Test
    void optimizationRejectsAnotherStudentResume() {
        when(resumeMapper.selectById(7L)).thenReturn(ownedResume(42L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> resumeService.optimizeForJob(99L, 7L, new ResumeOptimizationRequest(9L)));

        assertEquals(403, exception.getStatus().value());
        verify(jobMapper, never()).selectById(9L);
    }

    @Test
    void optimizationRequiresPublishedJob() {
        when(resumeMapper.selectById(7L)).thenReturn(ownedResume(42L));
        JobPosting draftJob = publishedJob();
        draftJob.setStatus("DRAFT");
        when(jobMapper.selectById(9L)).thenReturn(draftJob);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> resumeService.optimizeForJob(42L, 7L, new ResumeOptimizationRequest(9L)));

        assertEquals(404, exception.getStatus().value());
    }

    private ResumeRequest request() {
        return new ResumeRequest("Java 后端简历", "本科", "实习经历", "项目经历", "Java, Spring Boot", "认真负责");
    }

    private Resume ownedResume(Long studentId) {
        Resume resume = new Resume();
        resume.setId(7L);
        resume.setStudentId(studentId);
        resume.setTitle("Java 后端简历");
        resume.setProjectExperience("招聘平台 Java 后端开发");
        resume.setSkills("Java MySQL");
        resume.setStatus("DRAFT");
        return resume;
    }

    private JobPosting publishedJob() {
        JobPosting job = new JobPosting();
        job.setId(9L);
        job.setTitle("Java 开发工程师");
        job.setDescription("负责 Java 后端服务");
        job.setRequirements("Spring Boot MySQL 经验");
        job.setEmploymentType("FULL_TIME");
        job.setStatus("PUBLISHED");
        return job;
    }
}
