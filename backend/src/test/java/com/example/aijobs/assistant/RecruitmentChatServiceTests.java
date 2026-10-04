package com.example.aijobs.assistant;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.aijobs.application.entity.JobApplication;
import com.example.aijobs.application.mapper.JobApplicationMapper;
import com.example.aijobs.common.BusinessException;
import com.example.aijobs.deepseek.DeepSeekChatClient;
import com.example.aijobs.deepseek.DeepSeekChatResult;
import com.example.aijobs.deepseek.DeepSeekMessage;
import com.example.aijobs.deepseek.DeepSeekProperties;
import com.example.aijobs.job.entity.JobPosting;
import com.example.aijobs.job.mapper.JobPostingMapper;
import com.example.aijobs.match.entity.AiMatchResult;
import com.example.aijobs.match.mapper.AiMatchResultMapper;
import com.example.aijobs.resume.entity.Resume;
import com.example.aijobs.resume.mapper.ResumeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecruitmentChatServiceTests {
    @Mock private JobApplicationMapper applicationMapper;
    @Mock private JobPostingMapper jobMapper;
    @Mock private ResumeMapper resumeMapper;
    @Mock private AiMatchResultMapper matchMapper;
    private FakeDeepSeekChatClient chatClient;
    private RecruitmentChatService chatService;

    @BeforeEach
    void setUp() {
        chatClient = new FakeDeepSeekChatClient();
        chatService = new RecruitmentChatService(chatClient, applicationMapper, jobMapper, resumeMapper, matchMapper, 4);
    }

    @Test
    void studentChatUsesOwnedApplicationContextAndKeepsMatchScoreReadOnly() {
        when(applicationMapper.selectById(11L)).thenReturn(application(42L, 9L, 5L));
        when(jobMapper.selectById(9L)).thenReturn(job(7L));
        when(resumeMapper.selectById(5L)).thenReturn(resume(42L));
        when(matchMapper.selectOne(any(Wrapper.class))).thenReturn(match());
        chatClient.next = new DeepSeekChatResult(DeepSeekChatResult.Status.GENERATED, "请先补齐 Redis 证据。",
                "deepseek-flash", "stop", null, 200);

        RecruitmentChatResponse response = chatService.chat(42L, ChatRole.STUDENT,
                new RecruitmentChatRequest(null, "我该怎么准备？", 11L, null, null));

        assertTrue(response.generated());
        assertEquals("请先补齐 Redis 证据。", response.assistantMessage());
        assertEquals(new BigDecimal("66.67"), response.readOnlyMatchScore());
        assertEquals("ai-match-result", response.matchScoreSource());
        assertTrue(response.humanReviewRequired());
        assertTrue(response.contextSummary().contains("投递 11"));
        assertTrue(response.warnings().contains("匹配分只读展示，生成文本不能改写分数或招聘状态。"));
        assertTrue(chatClient.lastMessages.stream().anyMatch(message -> message.content().contains("最终招聘决定必须由人工复核")));
        assertTrue(chatClient.lastMessages.stream().anyMatch(message -> message.content().contains("只读匹配分：66.67")));
    }

    @Test
    void disabledModelReturnsExplicitFallbackAndStoresHistoryForSameOwner() {
        when(applicationMapper.selectById(11L)).thenReturn(application(42L, 9L, 5L));
        when(jobMapper.selectById(9L)).thenReturn(job(7L));
        when(resumeMapper.selectById(5L)).thenReturn(resume(42L));
        when(matchMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        chatClient.next = fallback(DeepSeekChatResult.Status.DISABLED);

        RecruitmentChatResponse first = chatService.chat(42L, ChatRole.STUDENT,
                new RecruitmentChatRequest(null, "第一轮", 11L, null, null));
        RecruitmentChatResponse second = chatService.chat(42L, ChatRole.STUDENT,
                new RecruitmentChatRequest(first.conversationId(), "第二轮", 11L, null, null));

        assertFalse(first.generated());
        assertTrue(first.assistantMessage().contains("外部模型当前未生成文本"));
        assertEquals(4, second.historyMessageCount());
        assertTrue(chatClient.lastMessages.stream().anyMatch(message -> message.content().contains("第一轮")));
        assertTrue(second.warnings().contains("外部模型未生成，已返回明确降级说明。"));
    }

    @Test
    void conversationCannotBeContinuedByAnotherUserOrRole() {
        chatClient.next = fallback(DeepSeekChatResult.Status.DISABLED);
        RecruitmentChatResponse first = chatService.chat(42L, ChatRole.STUDENT,
                new RecruitmentChatRequest(null, "你好", null, null, null));

        BusinessException otherUser = assertThrows(BusinessException.class, () -> chatService.chat(99L, ChatRole.STUDENT,
                new RecruitmentChatRequest(first.conversationId(), "越权", null, null, null)));
        BusinessException otherRole = assertThrows(BusinessException.class, () -> chatService.chat(42L, ChatRole.HR,
                new RecruitmentChatRequest(first.conversationId(), "越权", null, null, null)));

        assertEquals(403, otherUser.getStatus().value());
        assertEquals(403, otherRole.getStatus().value());
    }

    @Test
    void studentCannotReadAnotherStudentResumeContext() {
        when(resumeMapper.selectById(5L)).thenReturn(resume(42L));

        BusinessException exception = assertThrows(BusinessException.class, () -> chatService.chat(99L, ChatRole.STUDENT,
                new RecruitmentChatRequest(null, "看简历", null, null, 5L)));

        assertEquals(403, exception.getStatus().value());
        assertTrue(chatClient.lastMessages.isEmpty());
    }

    @Test
    void hrChatCanReadOnlyOwnedApplicationCandidateContext() {
        when(applicationMapper.selectById(11L)).thenReturn(application(42L, 9L, 5L));
        when(jobMapper.selectById(9L)).thenReturn(job(7L));
        when(resumeMapper.selectById(5L)).thenReturn(resume(42L));
        chatClient.next = new DeepSeekChatResult(DeepSeekChatResult.Status.GENERATED, "先人工核验项目。",
                "deepseek-flash", "stop", null, 200);

        RecruitmentChatResponse response = chatService.chat(7L, ChatRole.HR,
                new RecruitmentChatRequest(null, "如何复核？", 11L, null, null));

        assertEquals("HR", response.role());
        assertTrue(response.contextSummary().contains("岗位 9"));
        assertTrue(chatClient.lastMessages.stream().anyMatch(message -> message.content().contains("HR 招聘复核")));
        assertTrue(chatClient.lastMessages.stream().anyMatch(message -> message.content().contains("简历标题")));
    }

    @Test
    void hrCannotReadUnownedApplicationCandidateContext() {
        when(applicationMapper.selectById(11L)).thenReturn(application(42L, 9L, 5L));
        when(jobMapper.selectById(9L)).thenReturn(job(7L));

        BusinessException exception = assertThrows(BusinessException.class, () -> chatService.chat(99L, ChatRole.HR,
                new RecruitmentChatRequest(null, "如何复核？", 11L, null, null)));

        assertEquals(403, exception.getStatus().value());
        verify(resumeMapper, never()).selectById(5L);
        assertTrue(chatClient.lastMessages.isEmpty());
    }

    @Test
    void userMessageIsRequired() {
        BusinessException exception = assertThrows(BusinessException.class, () -> chatService.chat(42L, ChatRole.STUDENT,
                new RecruitmentChatRequest(null, "   ", null, null, null)));

        assertEquals(400, exception.getStatus().value());
    }

    private JobApplication application(Long studentId, Long jobId, Long resumeId) {
        JobApplication application = new JobApplication();
        application.setId(11L);
        application.setStudentId(studentId);
        application.setJobId(jobId);
        application.setResumeId(resumeId);
        application.setStatus("SUBMITTED");
        return application;
    }

    private JobPosting job(Long hrId) {
        JobPosting job = new JobPosting();
        job.setId(9L);
        job.setHrId(hrId);
        job.setTitle("Java 助教");
        job.setCompanyName("国际学校");
        job.setCity("上海");
        job.setStatus("PUBLISHED");
        job.setRequirements("Java Spring Boot Redis English");
        return job;
    }

    private Resume resume(Long studentId) {
        Resume resume = new Resume();
        resume.setId(5L);
        resume.setStudentId(studentId);
        resume.setTitle("后端助教简历");
        resume.setSkills("Java Spring Boot English");
        resume.setProjectExperience("AI recruitment platform");
        resume.setSourceText("Imported CV text");
        resume.setStatus("PUBLISHED");
        return resume;
    }

    private AiMatchResult match() {
        AiMatchResult match = new AiMatchResult();
        match.setJobId(9L);
        match.setResumeId(5L);
        match.setScore(new BigDecimal("66.67"));
        match.setModelName("local-skill-coverage-v1");
        match.setStrengthSummary("Java、Spring Boot、English");
        match.setGapSummary("Redis");
        return match;
    }

    private DeepSeekChatResult fallback(DeepSeekChatResult.Status status) {
        return new DeepSeekChatResult(status, "", "deepseek-flash", null, null, null);
    }

    private static final class FakeDeepSeekChatClient extends DeepSeekChatClient {
        private DeepSeekChatResult next = new DeepSeekChatResult(DeepSeekChatResult.Status.DISABLED,
                "", "deepseek-flash", null, null, null);
        private List<DeepSeekMessage> lastMessages = List.of();

        private FakeDeepSeekChatClient() {
            super(new DeepSeekProperties(false, "", "deepseek-flash", Duration.ofSeconds(1),
                    Duration.ofSeconds(1), 128), new ObjectMapper());
        }

        @Override
        public DeepSeekChatResult generate(List<DeepSeekMessage> messages) {
            lastMessages = new ArrayList<>(messages);
            return next;
        }
    }
}
