package com.example.aijobs.evaluation;

import com.example.aijobs.deepseek.DeepSeekChatClient;
import com.example.aijobs.deepseek.DeepSeekChatResult;
import com.example.aijobs.deepseek.DeepSeekMessage;
import com.example.aijobs.deepseek.DeepSeekProperties;
import com.example.aijobs.match.SkillCoverageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ControlledComparisonServiceTests {

    @Test
    void disabledModelStillReturnsRuleBaselineAndLimitations() {
        FakeDeepSeekChatClient client = new FakeDeepSeekChatClient(fallback(DeepSeekChatResult.Status.DISABLED));
        ControlledComparisonReport report = new ControlledComparisonService(client, new SkillCoverageService()).runDay30Comparison();

        assertEquals("day30-controlled-comparison-2026-10-06", report.evaluationId());
        assertEquals(ControlledComparisonService.SCENARIO_SET_VERSION, report.scenarioSetVersion());
        assertSha256(report.scenarioSetHash());
        assertEquals("rule-plus-fallback", report.runMode());
        assertEquals(3, report.scenarioCount());
        assertEquals(0, report.generatedCount());
        assertEquals(3, report.fallbackCount());
        assertTrue(report.humanReviewRequired());
        assertTrue(report.summary().contains("未生成文本"));
        assertTrue(report.limitations().stream().anyMatch(item -> item.contains("不能声称完成真实模型增益评估")));

        ControlledComparisonResult first = report.results().getFirst();
        assertSha256(first.inputHash());
        assertSha256(first.ruleEvidenceHash());
        assertSha256(first.modelPromptHash());
        assertEquals(new BigDecimal("66.67"), first.ruleScore());
        assertEquals(List.of("Lesson planning", "English"), first.matchedSkills());
        assertEquals(List.of("Classroom management"), first.missingSkills());
        assertFalse(first.modelGenerated());
        assertTrue(first.ruleOnlySummary().contains("不代表录用概率"));
        assertTrue(first.riskFlags().contains("模型未生成，不能评估额外贡献。"));
        assertEquals(3, client.calls.size());
        assertTrue(client.calls.getFirst().stream().anyMatch(message -> message.content().contains("不得改写分数")));
        assertTrue(client.calls.getFirst().stream().anyMatch(message -> message.content().contains(first.resumeText())
                && message.content().contains(first.jobRequirements())));
        ControlledComparisonResult fullCoverage = report.results().getLast();
        assertTrue(fullCoverage.contributionChecks().stream().anyMatch(item -> item.contains("不适用")));
        assertFalse(fullCoverage.contributionChecks().stream().anyMatch(item -> item.contains("提到至少一个缺口")));
        assertTrue(report.reproducibilityNotes().stream().anyMatch(item -> item.contains("latencyMs")));
    }

    @Test
    void generatedTextIsCheckedWithoutChangingRuleScore() {
        FakeDeepSeekChatClient client = new FakeDeepSeekChatClient(new DeepSeekChatResult(
                DeepSeekChatResult.Status.GENERATED,
                "建议人工复核 Classroom management 缺口，并追问真实课堂案例。",
                "deepseek-flash",
                "stop",
                new DeepSeekChatResult.Usage(30, 20, 50),
                200));

        ControlledComparisonReport report = new ControlledComparisonService(client, new SkillCoverageService()).runDay30Comparison();

        ControlledComparisonResult first = report.results().getFirst();
        assertEquals("rule-plus-model-output", report.runMode());
        assertTrue(report.limitations().stream().anyMatch(item -> item.contains("真实调用须另有执行证据")));
        assertEquals(3, report.generatedCount());
        assertEquals(new BigDecimal("66.67"), first.ruleScore());
        assertTrue(first.modelGenerated());
        assertEquals(30, first.promptTokens());
        assertEquals(20, first.completionTokens());
        assertEquals(50, first.totalTokens());
        assertEquals(first.ruleEvidenceHash(), report.results().getFirst().ruleEvidenceHash());
        assertTrue(first.contributionChecks().stream().anyMatch(item -> item.contains("模型文本提到至少一个缺口")));
        assertTrue(first.contributionChecks().stream().anyMatch(item -> item.contains("模型文本提示人工核验")));
        assertTrue(first.riskFlags().contains("未发现自动决策或改分表述；仍需人工核验真实性。"));
    }

    @Test
    void generatedTextThatOverstepsDecisionBoundaryIsFlagged() {
        FakeDeepSeekChatClient client = new FakeDeepSeekChatClient(new DeepSeekChatResult(
                DeepSeekChatResult.Status.GENERATED,
                "建议直接录用，并把分数修改分数到 100。",
                "deepseek-flash",
                "stop",
                null,
                200));

        ControlledComparisonResult first = new ControlledComparisonService(client, new SkillCoverageService())
                .runDay30Comparison().results().getFirst();

        assertTrue(first.riskFlags().contains("模型文本可能越过人工招聘决定边界。"));
        assertTrue(first.riskFlags().contains("模型文本可能尝试改写规则分数。"));
        assertTrue(first.riskFlags().contains("模型文本遗漏规则缺口，需人工复核。"));
    }

    @Test
    void partialGenerationKeepsPerScenarioFallbackAndDoesNotClaimACompleteModelRun() {
        FakeDeepSeekChatClient client = new FakeDeepSeekChatClient(
                new DeepSeekChatResult(DeepSeekChatResult.Status.GENERATED,
                        "人工复核 Classroom management", "deepseek-flash", "stop", null, 200),
                fallback(DeepSeekChatResult.Status.TIMEOUT),
                fallback(DeepSeekChatResult.Status.UPSTREAM_ERROR));
        ControlledComparisonReport report = new ControlledComparisonService(client, new SkillCoverageService())
                .runDay30Comparison();

        assertEquals("rule-plus-partial-output", report.runMode());
        assertEquals(1, report.generatedCount());
        assertEquals(2, report.fallbackCount());
        assertTrue(report.summary().contains("不能视为完整模型比较"));
        assertEquals(List.of(new BigDecimal("66.67"), new BigDecimal("75.00"), new BigDecimal("100.00")),
                report.results().stream().map(ControlledComparisonResult::ruleScore).toList());
        assertEquals(DeepSeekChatResult.Status.TIMEOUT, report.results().get(1).modelStatus());
        assertEquals(DeepSeekChatResult.Status.UPSTREAM_ERROR, report.results().get(2).modelStatus());
        assertNull(report.results().get(1).totalTokens());
    }

    @Test
    void reproducibilityHashesStayStableAcrossFallbackAndGeneratedRuns() {
        ControlledComparisonReport fallbackReport = new ControlledComparisonService(
                new FakeDeepSeekChatClient(fallback(DeepSeekChatResult.Status.DISABLED)), new SkillCoverageService())
                .runDay30Comparison();
        ControlledComparisonReport generatedReport = new ControlledComparisonService(
                new FakeDeepSeekChatClient(new DeepSeekChatResult(DeepSeekChatResult.Status.GENERATED,
                        "人工复核 Classroom management", "deepseek-flash", "stop", null, 200)),
                new SkillCoverageService()).runDay30Comparison();

        assertEquals(fallbackReport.scenarioSetHash(), generatedReport.scenarioSetHash());
        assertEquals(fallbackReport.results().stream().map(ControlledComparisonResult::inputHash).toList(),
                generatedReport.results().stream().map(ControlledComparisonResult::inputHash).toList());
        assertEquals(fallbackReport.results().stream().map(ControlledComparisonResult::ruleEvidenceHash).toList(),
                generatedReport.results().stream().map(ControlledComparisonResult::ruleEvidenceHash).toList());
        assertEquals(fallbackReport.results().stream().map(ControlledComparisonResult::modelPromptHash).toList(),
                generatedReport.results().stream().map(ControlledComparisonResult::modelPromptHash).toList());
    }

    private void assertSha256(String value) {
        assertNotNull(value);
        assertTrue(value.matches("[0-9a-f]{64}"), () -> "Not a SHA-256 hex digest: " + value);
    }

    private static DeepSeekChatResult fallback(DeepSeekChatResult.Status status) {
        return new DeepSeekChatResult(status, "", "deepseek-flash", null, null, null);
    }

    private static final class FakeDeepSeekChatClient extends DeepSeekChatClient {
        private final List<DeepSeekChatResult> next;
        private final List<List<DeepSeekMessage>> calls = new ArrayList<>();

        private FakeDeepSeekChatClient(DeepSeekChatResult... next) {
            super(new DeepSeekProperties(false, "", "deepseek-flash", Duration.ofSeconds(1),
                    Duration.ofSeconds(1), 128), new ObjectMapper());
            this.next = List.of(next);
        }

        @Override
        public DeepSeekChatResult generate(List<DeepSeekMessage> messages) {
            calls.add(new ArrayList<>(messages));
            return next.get(Math.min(calls.size() - 1, next.size() - 1));
        }
    }
}
