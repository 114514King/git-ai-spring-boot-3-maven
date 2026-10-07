package com.example.aijobs.evaluation;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.aijobs.application.mapper.JobApplicationMapper;
import com.example.aijobs.deepseek.DeepSeekChatClient;
import com.example.aijobs.deepseek.DeepSeekChatResult;
import com.example.aijobs.job.entity.JobPosting;
import com.example.aijobs.job.mapper.JobPostingMapper;
import com.example.aijobs.match.AiMatchService;
import com.example.aijobs.match.SkillCoverageService;
import com.example.aijobs.match.dto.MatchRequest;
import com.example.aijobs.match.dto.MatchResponse;
import com.example.aijobs.match.entity.AiMatchResult;
import com.example.aijobs.match.mapper.AiMatchResultMapper;
import com.example.aijobs.resume.entity.Resume;
import com.example.aijobs.resume.mapper.ResumeMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ControlledComparisonBaselineTests {
    @Mock private AiMatchResultMapper matchMapper;
    @Mock private ResumeMapper resumeMapper;
    @Mock private JobPostingMapper jobMapper;
    @Mock private JobApplicationMapper applicationMapper;

    @ParameterizedTest
    @MethodSource("scenarios")
    void evaluationInputMatchesBothBusinessPathsAndPersistedEvidence(ControlledComparisonResult scenario) {
        Resume resume = new Resume();
        resume.setId(5L);
        resume.setStudentId(42L);
        resume.setStatus("PUBLISHED");
        resume.setTitle("Synthetic test resume");
        resume.setSourceText(scenario.resumeText());
        JobPosting job = new JobPosting();
        job.setId(9L);
        job.setHrId(7L);
        job.setStatus("PUBLISHED");
        job.setRequirements(scenario.jobRequirements());
        when(resumeMapper.selectById(5L)).thenReturn(resume);
        when(jobMapper.selectById(9L)).thenReturn(job);
        when(applicationMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        AiMatchService service = new AiMatchService(matchMapper, resumeMapper, jobMapper, applicationMapper,
                new SkillCoverageService());

        MatchResponse student = service.matchForStudent(42L, new MatchRequest(5L, 9L));
        MatchResponse hr = service.matchForHr(7L, new MatchRequest(5L, 9L));

        ArgumentCaptor<AiMatchResult> saved = ArgumentCaptor.forClass(AiMatchResult.class);
        verify(matchMapper, times(2)).insert(saved.capture());
        for (MatchResponse response : List.of(student, hr)) {
            assertEquals(scenario.ruleScore(), response.score());
            assertEquals(SkillCoverageService.MODEL_NAME, response.modelName());
            assertTrue(response.analysis().contains(scenario.matchedSkills().size() + "/"
                    + scenario.requiredSkills().size() + " × 100 = " + scenario.ruleScore() + "%"));
            assertTrue(response.analysis().contains(String.join("、", scenario.requiredSkills())));
            assertEquals("简历文本已识别的岗位技能：" + String.join("、", scenario.matchedSkills()) + "。",
                    response.strengthSummary());
            assertEquals(scenario.missingSkills().isEmpty()
                            ? "已覆盖词典识别到的全部岗位技能；仍需人工核验熟练度、证据与未收录要求。"
                            : "简历文本尚未识别的岗位技能：" + String.join("、", scenario.missingSkills()) + "；未提及不等于不具备。",
                    response.gapSummary());
        }
        for (AiMatchResult result : saved.getAllValues()) {
            assertEquals(scenario.ruleScore(), result.getScore());
            assertEquals(student.strengthSummary(), result.getStrengthSummary());
            assertEquals(student.gapSummary(), result.getGapSummary());
        }
        verify(applicationMapper).selectCount(any(Wrapper.class));
        verifyNoMoreInteractions(applicationMapper);
    }

    private static Stream<ControlledComparisonResult> scenarios() {
        DeepSeekChatClient client = mock(DeepSeekChatClient.class);
        when(client.generate(any())).thenReturn(new DeepSeekChatResult(
                DeepSeekChatResult.Status.DISABLED, "", "deepseek-flash", null, null, null));
        return new ControlledComparisonService(client, new SkillCoverageService()).runDay30Comparison().results().stream();
    }
}
