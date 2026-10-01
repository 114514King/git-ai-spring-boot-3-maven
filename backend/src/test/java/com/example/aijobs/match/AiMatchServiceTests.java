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
        assertEquals(new BigDecimal("66.67"), response.score());
        assertEquals("local-skill-coverage-v1", response.modelName());
        assertTrue(response.analysis().contains("2/3 × 100 = 66.67%"));
        assertTrue(response.strengthSummary().contains("Java、Spring Boot"));
        assertTrue(response.gapSummary().contains("Redis"));
        assertFalse(response.gapSummary().contains("开发工程师"));
        assertTrue(response.actionSuggestions().contains("缺口技能"));
        verifyNoInteractions(applicationMapper);
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
        assertEquals(new BigDecimal("66.67"), response.score());
        assertEquals("local-skill-coverage-v1", response.modelName());
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

    @Test
    void teachingAliasesProduceRequiredSkillCoverageAndCompleteGaps() {
        Resume resume = publishedResume(42L);
        resume.setSkills("英语 教学设计 备课");
        JobPosting job = publishedJob(7L);
        job.setRequirements("English, lesson planning, classroom management");
        MatchResponse response = match(resume, job);
        assertEquals(new BigDecimal("66.67"), response.score());
        assertTrue(response.strengthSummary().contains("Lesson planning、English"));
        assertTrue(response.gapSummary().contains("Classroom management"));
        assertTrue(response.analysis().contains("最终决定由人工复核"));
    }

    @Test
    void jobTitleDescriptionAndEmploymentTypeDoNotChangeDenominator() {
        Resume resume = publishedResume(42L);
        JobPosting job = publishedJob(7L);
        job.setTitle("Python English 助教");
        job.setDescription("Excel Git Linux");
        job.setEmploymentType("PART_TIME");
        job.setRequirements("Java java JAVA、SpringBoot spring boot");
        MatchResponse response = match(resume, job);
        assertEquals(new BigDecimal("100.00"), response.score());
        assertTrue(response.analysis().contains("2/2"));
        assertFalse(response.analysis().contains("Python"));
        assertTrue(response.gapSummary().contains("人工核验"));
    }

    @Test
    void emptyResumeHasZeroCoverageAndAllRequiredSkillsAsGaps() {
        Resume resume = new Resume();
        resume.setId(5L);
        resume.setStudentId(42L);
        resume.setStatus("PUBLISHED");
        resume.setTitle("Java Spring Boot Redis");
        MatchResponse response = match(resume, publishedJob(7L));
        assertEquals(new BigDecimal("0.00"), response.score());
        assertTrue(response.analysis().contains("0/3"));
        assertTrue(response.gapSummary().contains("Java、Spring Boot、Redis"));
    }

    @Test
    void noSupportedRequirementRejectsScoringWithoutOverwritingExistingResult() {
        Resume resume = publishedResume(42L);
        JobPosting job = publishedJob(7L);
        when(resumeMapper.selectById(5L)).thenReturn(resume);
        when(jobMapper.selectById(9L)).thenReturn(job);
        for (String requirements : new String[] { null, "", "认真负责 QuantumWidget" }) {
            job.setRequirements(requirements);
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> matchService.matchForStudent(42L, new MatchRequest(5L, 9L)));
            assertEquals(422, exception.getStatus().value());
        }
        verifyNoInteractions(matchMapper, applicationMapper);
    }

    @Test
    void doesNotCreateMultiwordSkillsAcrossResumeFields() {
        Resume resume = publishedResume(42L);
        resume.setWorkExperience("Spring");
        resume.setProjectExperience("Boot");
        resume.setSkills(null);
        JobPosting job = publishedJob(7L);
        job.setRequirements("Spring Boot");
        assertEquals(new BigDecimal("0.00"), match(resume, job).score());
    }

    @Test
    void historyRetainsOriginalModelAndScoreUntilRegenerated() {
        AiMatchResult old = new AiMatchResult();
        old.setResumeId(5L);
        old.setScore(new BigDecimal("33.33"));
        old.setModelName("local-keyword-match-v2");
        when(resumeMapper.selectById(5L)).thenReturn(publishedResume(42L));
        when(matchMapper.selectList(any(Wrapper.class))).thenReturn(java.util.List.of(old));
        MatchResponse response = matchService.listStudentMatches(42L, 5L).getFirst();
        assertEquals(new BigDecimal("33.33"), response.score());
        assertEquals("local-keyword-match-v2", response.modelName());
        verify(matchMapper, never()).updateById(any(AiMatchResult.class));
    }

    private MatchResponse match(Resume resume, JobPosting job) {
        when(resumeMapper.selectById(5L)).thenReturn(resume);
        when(jobMapper.selectById(9L)).thenReturn(job);
        return matchService.matchForStudent(42L, new MatchRequest(5L, 9L));
    }

    @Test
    void importedTextParticipatesInSkillCoverageWithoutDuplicateCredit() {
        Resume resume = publishedResume(42L);
        resume.setSkills("English");
        resume.setSourceText("英语 教学设计 备课");
        JobPosting job = publishedJob(7L);
        job.setRequirements("English, lesson planning, classroom management");
        MatchResponse response = match(resume, job);
        assertEquals(new BigDecimal("66.67"), response.score());
        assertTrue(response.analysis().contains("2/3"));
        assertTrue(response.gapSummary().contains("Classroom management"));
    }

    private JobPosting publishedJob(Long hrId) {
        JobPosting job = new JobPosting();
        job.setId(9L);
        job.setHrId(hrId);
        job.setTitle("Java 开发工程师");
        job.setDescription("负责 Java 后端服务");
        job.setRequirements("Java、Spring Boot、Redis 经验");
        job.setEmploymentType("FULL_TIME");
        job.setStatus("PUBLISHED");
        return job;
    }
}
