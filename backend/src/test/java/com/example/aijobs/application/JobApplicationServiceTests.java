package com.example.aijobs.application;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.aijobs.application.dto.ApplicationFollowUpAdviceResponse;
import com.example.aijobs.application.dto.ApplicationRequest;
import com.example.aijobs.application.dto.ApplicationResponse;
import com.example.aijobs.application.dto.CandidateCommunicationDraftResponse;
import com.example.aijobs.application.dto.CandidateRecommendationResponse;
import com.example.aijobs.application.dto.InterviewKitResponse;
import com.example.aijobs.application.dto.StudentApplicationActionPlanResponse;
import com.example.aijobs.application.entity.JobApplication;
import com.example.aijobs.application.mapper.JobApplicationMapper;
import com.example.aijobs.common.BusinessException;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTests {
    @Mock private JobApplicationMapper applicationMapper;
    @Mock private JobPostingMapper jobMapper;
    @Mock private ResumeMapper resumeMapper;
    @Mock private AiMatchResultMapper matchMapper;
    private JobApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new JobApplicationService(applicationMapper, jobMapper, resumeMapper, matchMapper);
    }

    @Test
    void studentCanSubmitWithPublishedOwnedResume() {
        when(jobMapper.selectById(9L)).thenReturn(publishedJob(7L));
        when(resumeMapper.selectById(5L)).thenReturn(publishedResume(42L));
        when(applicationMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            JobApplication application = invocation.getArgument(0);
            application.setId(11L);
            return 1;
        }).when(applicationMapper).insert(any(JobApplication.class));

        ApplicationResponse response = applicationService.submit(42L, new ApplicationRequest(9L, 5L));

        assertEquals(11L, response.id());
        assertEquals(9L, response.jobId());
        assertEquals(42L, response.studentId());
        assertEquals("SUBMITTED", response.status());
    }

    @Test
    void cannotSubmitDraftResume() {
        when(jobMapper.selectById(9L)).thenReturn(publishedJob(7L));
        Resume resume = publishedResume(42L);
        resume.setStatus("DRAFT");
        when(resumeMapper.selectById(5L)).thenReturn(resume);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> applicationService.submit(42L, new ApplicationRequest(9L, 5L)));

        assertEquals(400, exception.getStatus().value());
        verify(applicationMapper, never()).insert(any(JobApplication.class));
    }

    @Test
    void cannotSubmitSameJobTwice() {
        when(jobMapper.selectById(9L)).thenReturn(publishedJob(7L));
        when(resumeMapper.selectById(5L)).thenReturn(publishedResume(42L));
        when(applicationMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> applicationService.submit(42L, new ApplicationRequest(9L, 5L)));

        assertEquals(409, exception.getStatus().value());
    }

    @Test
    void hrCanListOnlyOwnedJobApplications() {
        JobPosting ownedJob = publishedJob(7L);
        ownedJob.setId(9L);
        when(jobMapper.selectList(any(Wrapper.class))).thenReturn(List.of(ownedJob));
        when(applicationMapper.selectList(any(Wrapper.class))).thenReturn(List.of(application(42L, 9L)));

        List<ApplicationResponse> response = applicationService.listHrApplications(7L, 9L);

        assertEquals(1, response.size());
        assertEquals(9L, response.getFirst().jobId());
    }

    @Test
    void hrRecommendationsUseExistingMatchScoreAndSortByScore() {
        JobPosting ownedJob = publishedJob(7L);
        ownedJob.setId(9L);
        ownedJob.setTitle("Java 后端工程师");
        ownedJob.setRequirements("Java Spring Boot MySQL Redis");
        JobApplication highApplication = application(42L, 9L);
        highApplication.setId(11L);
        JobApplication lowApplication = application(43L, 9L);
        lowApplication.setId(12L);
        lowApplication.setResumeId(6L);
        Resume highResume = publishedResume(42L);
        highResume.setSkills("Java Spring Boot MySQL");
        Resume lowResume = publishedResume(43L);
        lowResume.setId(6L);
        lowResume.setSkills("运营 内容");
        AiMatchResult match = new AiMatchResult();
        match.setScore(BigDecimal.valueOf(88));

        when(jobMapper.selectList(any(Wrapper.class))).thenReturn(List.of(ownedJob));
        when(applicationMapper.selectList(any(Wrapper.class))).thenReturn(List.of(lowApplication, highApplication));
        when(jobMapper.selectById(9L)).thenReturn(ownedJob);
        when(resumeMapper.selectById(5L)).thenReturn(highResume);
        when(resumeMapper.selectById(6L)).thenReturn(lowResume);
        when(matchMapper.selectOne(any(Wrapper.class))).thenReturn(null, match);

        List<CandidateRecommendationResponse> recommendations =
                applicationService.recommendHrCandidates(7L, 9L);

        assertEquals(2, recommendations.size());
        assertEquals(11L, recommendations.getFirst().applicationId());
        assertEquals(BigDecimal.valueOf(88), recommendations.getFirst().recommendationScore());
        assertEquals("ai-match-result", recommendations.getFirst().scoreSource());
        assertTrue(recommendations.getFirst().suggestedAction().contains("面试"));
    }

    @Test
    void hrCannotReadRecommendationsForUnownedJob() {
        JobPosting ownedJob = publishedJob(7L);
        ownedJob.setId(9L);
        when(jobMapper.selectList(any(Wrapper.class))).thenReturn(List.of(ownedJob));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> applicationService.recommendHrCandidates(7L, 10L));

        assertEquals(403, exception.getStatus().value());
        verify(applicationMapper, never()).selectList(any(Wrapper.class));
    }

    @Test
    void hrCanGenerateInterviewKitForOwnedApplication() {
        JobPosting job = publishedJob(7L);
        job.setTitle("Java Backend Engineer");
        job.setRequirements("Java Spring Boot MySQL Redis");
        Resume resume = publishedResume(42L);
        resume.setTitle("Java Resume");
        resume.setSkills("Java Spring Boot MySQL");
        resume.setProjectExperience("AI recruitment platform");
        when(applicationMapper.selectById(11L)).thenReturn(application(42L, 9L));
        when(jobMapper.selectById(9L)).thenReturn(job);
        when(resumeMapper.selectById(5L)).thenReturn(resume);

        InterviewKitResponse response = applicationService.generateInterviewKit(7L, 11L);

        assertEquals("local-interview-kit-v1", response.modelName());
        assertEquals(4, response.questions().size());
        assertEquals(4, response.scoringDimensions().size());
        assertTrue(response.questions().getFirst().question().contains("java"));
        assertEquals(100, response.scoringDimensions().stream().mapToInt(InterviewKitResponse.ScoringDimension::weight).sum());
    }

    @Test
    void hrCanGenerateFollowUpAdviceForOwnedApplication() {
        JobPosting job = publishedJob(7L);
        job.setTitle("Java Backend Engineer");
        job.setRequirements("Java Spring Boot MySQL Redis");
        Resume resume = publishedResume(42L);
        resume.setTitle("Java Resume");
        resume.setSkills("Java Spring Boot MySQL");
        AiMatchResult match = new AiMatchResult();
        match.setScore(BigDecimal.valueOf(82));
        when(applicationMapper.selectById(11L)).thenReturn(application(42L, 9L));
        when(jobMapper.selectById(9L)).thenReturn(job);
        when(resumeMapper.selectById(5L)).thenReturn(resume);
        when(matchMapper.selectOne(any(Wrapper.class))).thenReturn(match);

        ApplicationFollowUpAdviceResponse response = applicationService.generateFollowUpAdvice(7L, 11L);

        assertEquals("local-application-follow-up-v1", response.modelName());
        assertEquals(BigDecimal.valueOf(82), response.matchScore());
        assertEquals("ai-match-result", response.scoreSource());
        assertEquals("HIGH", response.priorityLevel());
        assertEquals("INTERVIEW", response.nextStatusSuggestion());
        assertTrue(response.recommendedActions().getFirst().contains("面试"));
    }

    @Test
    void hrCanGenerateCommunicationDraftForOwnedApplication() {
        JobPosting job = publishedJob(7L);
        job.setTitle("Java Backend Engineer");
        job.setRequirements("Java Spring Boot MySQL Redis");
        Resume resume = publishedResume(42L);
        resume.setTitle("Java Resume");
        resume.setSkills("Java Spring Boot MySQL");
        AiMatchResult match = new AiMatchResult();
        match.setScore(BigDecimal.valueOf(86));
        when(applicationMapper.selectById(11L)).thenReturn(application(42L, 9L));
        when(jobMapper.selectById(9L)).thenReturn(job);
        when(resumeMapper.selectById(5L)).thenReturn(resume);
        when(matchMapper.selectOne(any(Wrapper.class))).thenReturn(match);

        CandidateCommunicationDraftResponse response = applicationService.generateCommunicationDraft(7L, 11L);

        assertEquals("local-candidate-communication-draft-v1", response.modelName());
        assertEquals(BigDecimal.valueOf(86), response.matchScore());
        assertEquals("ai-match-result", response.scoreSource());
        assertEquals("FAST_TRACK_INVITATION", response.communicationScenario());
        assertTrue(response.openingMessage().contains("Java Backend Engineer"));
        assertEquals(3, response.keyQuestions().size());
        assertFalse(response.nextActions().isEmpty());
    }

    @Test
    void studentCanGenerateActionPlanForOwnApplication() {
        JobPosting job = publishedJob(7L);
        job.setTitle("Java Backend Engineer");
        job.setRequirements("Java Spring Boot MySQL Redis");
        Resume resume = publishedResume(42L);
        resume.setTitle("Java Resume");
        resume.setSkills("Java Spring Boot MySQL");
        AiMatchResult match = new AiMatchResult();
        match.setScore(BigDecimal.valueOf(78));
        when(applicationMapper.selectById(11L)).thenReturn(application(42L, 9L));
        when(jobMapper.selectById(9L)).thenReturn(job);
        when(resumeMapper.selectById(5L)).thenReturn(resume);
        when(matchMapper.selectOne(any(Wrapper.class))).thenReturn(match);

        StudentApplicationActionPlanResponse response = applicationService.generateStudentActionPlan(42L, 11L);

        assertEquals("local-student-action-plan-v1", response.modelName());
        assertEquals(BigDecimal.valueOf(78), response.matchScore());
        assertEquals("ai-match-result", response.scoreSource());
        assertEquals("HIGH", response.priorityLevel());
        assertTrue(response.statusSummary().contains("匹配度较高"));
        assertFalse(response.preparationChecklist().isEmpty());
        assertFalse(response.nextActions().isEmpty());
    }

    @Test
    void studentCannotGenerateActionPlanForOthersApplication() {
        when(applicationMapper.selectById(11L)).thenReturn(application(42L, 9L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> applicationService.generateStudentActionPlan(99L, 11L));

        assertEquals(403, exception.getStatus().value());
        verify(jobMapper, never()).selectById(any());
    }

    @Test
    void hrCannotGenerateFollowUpAdviceForUnownedApplication() {
        when(applicationMapper.selectById(11L)).thenReturn(application(42L, 9L));
        when(jobMapper.selectById(9L)).thenReturn(publishedJob(7L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> applicationService.generateFollowUpAdvice(99L, 11L));

        assertEquals(403, exception.getStatus().value());
        verify(resumeMapper, never()).selectById(any());
    }

    @Test
    void hrCannotGenerateCommunicationDraftForUnownedApplication() {
        when(applicationMapper.selectById(11L)).thenReturn(application(42L, 9L));
        when(jobMapper.selectById(9L)).thenReturn(publishedJob(7L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> applicationService.generateCommunicationDraft(99L, 11L));

        assertEquals(403, exception.getStatus().value());
        verify(resumeMapper, never()).selectById(any());
    }

    @Test
    void hrCannotGenerateInterviewKitForUnownedApplication() {
        when(applicationMapper.selectById(11L)).thenReturn(application(42L, 9L));
        when(jobMapper.selectById(9L)).thenReturn(publishedJob(7L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> applicationService.generateInterviewKit(99L, 11L));

        assertEquals(403, exception.getStatus().value());
        verify(resumeMapper, never()).selectById(any());
    }

    @Test
    void anotherHrCannotUpdateApplicationStatus() {
        when(applicationMapper.selectById(11L)).thenReturn(application(42L, 9L));
        when(jobMapper.selectById(9L)).thenReturn(publishedJob(7L));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> applicationService.updateHrStatus(99L, 11L, "REVIEWING"));

        assertEquals(403, exception.getStatus().value());
    }

    @Test
    void studentCanWithdrawOwnApplication() {
        when(applicationMapper.selectById(11L)).thenReturn(application(42L, 9L));

        ApplicationResponse response = applicationService.withdraw(42L, 11L);

        assertEquals("WITHDRAWN", response.status());
        verify(applicationMapper).updateById(any(JobApplication.class));
    }

    private JobPosting publishedJob(Long hrId) {
        JobPosting job = new JobPosting();
        job.setId(9L);
        job.setHrId(hrId);
        job.setStatus("PUBLISHED");
        return job;
    }

    private Resume publishedResume(Long studentId) {
        Resume resume = new Resume();
        resume.setId(5L);
        resume.setStudentId(studentId);
        resume.setStatus("PUBLISHED");
        return resume;
    }

    private JobApplication application(Long studentId, Long jobId) {
        JobApplication application = new JobApplication();
        application.setId(11L);
        application.setJobId(jobId);
        application.setStudentId(studentId);
        application.setResumeId(5L);
        application.setStatus("SUBMITTED");
        return application;
    }
}
