package com.example.aijobs.evaluation;

import com.example.aijobs.deepseek.DeepSeekChatClient;
import com.example.aijobs.deepseek.DeepSeekChatResult;
import com.example.aijobs.deepseek.DeepSeekMessage;
import com.example.aijobs.match.SkillNormalizer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ControlledComparisonService {
    private static final String RULE_MODEL_NAME = "local-skill-coverage-v1";

    private final DeepSeekChatClient deepSeekChatClient;

    public ControlledComparisonService(DeepSeekChatClient deepSeekChatClient) {
        this.deepSeekChatClient = deepSeekChatClient;
    }

    public ControlledComparisonReport runDay30Comparison() {
        List<ControlledComparisonResult> results = scenarios().stream()
                .map(this::runScenario)
                .toList();
        long generated = results.stream().filter(ControlledComparisonResult::modelGenerated).count();
        long fallback = results.size() - generated;
        String summary = "固定合成输入完成规则基线与规则加模型辅助对照；规则分数始终由本地算法给出，模型文本只作为人工复核材料。";
        if (generated == 0) {
            summary += " 本次外部模型未生成文本，只验证降级链路和评估记录结构。";
        }
        return new ControlledComparisonReport(
                "day30-controlled-comparison-2026-10-06",
                generated > 0 ? "rule-plus-live-model" : "rule-plus-fallback",
                RULE_MODEL_NAME,
                SkillNormalizer.VERSION,
                results.size(),
                Math.toIntExact(generated),
                Math.toIntExact(fallback),
                true,
                summary,
                results,
                List.of(
                        "全部场景均为合成招聘数据，不代表真实候选人或真实岗位效果。",
                        "模型输出不参与分数计算，不自动修改投递状态，也不形成录用或拒绝决定。",
                        "缺少 DeepSeek 凭证或开关关闭时，只能验证降级行为，不能声称完成真实模型增益评估。",
                        "即使启用真实模型，本接口也只报告小样本检查结果，不能外推为招聘准确率或效率提升。"));
    }

    private ControlledComparisonResult runScenario(Scenario scenario) {
        ScoreEvidence evidence = score(scenario.resumeText(), scenario.jobRequirements());
        String ruleOnly = ruleOnlySummary(evidence);
        List<DeepSeekMessage> messages = List.of(
                new DeepSeekMessage("system", systemPrompt()),
                new DeepSeekMessage("user", userPrompt(scenario, evidence, ruleOnly)));
        Instant startedAt = Instant.now();
        DeepSeekChatResult model = deepSeekChatClient.generate(messages);
        long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
        String modelSummary = model.generated()
                ? model.content()
                : "外部模型未生成文本（状态：" + model.status() + "）；本场景仅保留规则基线、缺口和人工复核要求。";
        List<String> checks = contributionChecks(model, evidence);
        List<String> risks = riskFlags(model, evidence);
        DeepSeekChatResult.Usage usage = model.usage();
        return new ControlledComparisonResult(
                scenario.id(),
                scenario.role(),
                evidence.score(),
                List.copyOf(evidence.required()),
                List.copyOf(evidence.matched()),
                List.copyOf(evidence.missing()),
                ruleOnly,
                modelSummary,
                model.generated(),
                model.status(),
                model.model(),
                latencyMs,
                usage == null ? null : usage.promptTokens(),
                usage == null ? null : usage.completionTokens(),
                usage == null ? null : usage.totalTokens(),
                checks,
                risks);
    }

    private ScoreEvidence score(String resumeText, String requirements) {
        Set<String> required = SkillNormalizer.normalize(requirements);
        Set<String> resume = SkillNormalizer.normalize(resumeText);
        Set<String> matched = new LinkedHashSet<>(required);
        matched.retainAll(resume);
        Set<String> missing = new LinkedHashSet<>(required);
        missing.removeAll(matched);
        BigDecimal score = required.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(matched.size()).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(required.size()), 2, RoundingMode.HALF_UP);
        return new ScoreEvidence(required, matched, missing, score);
    }

    private String ruleOnlySummary(ScoreEvidence evidence) {
        String gapText = evidence.missing().isEmpty()
                ? "无词典内缺口，仍需人工核验证据和熟练度"
                : String.join("、", evidence.missing());
        return "规则基线：" + evidence.matched().size() + "/" + evidence.required().size()
                + " = " + evidence.score().toPlainString() + "%；缺口：" + gapText
                + "；该分数不代表录用概率。";
    }

    private String systemPrompt() {
        return """
                你是招聘系统受控评估助手。只能解释服务端给出的规则分、匹配技能和缺口。
                不得改写分数，不得输出录用或拒绝决定，不得声称真实招聘效果或模型准确率。
                回答要标出模型能额外提供的人工复核问题、风险和下一步核验动作。
                """.strip();
    }

    private String userPrompt(Scenario scenario, ScoreEvidence evidence, String ruleOnly) {
        return """
                场景：%s
                角色：%s
                岗位要求：%s
                简历文本：%s
                %s
                请在不改变分数的前提下，说明规则加模型相对规则基线能否提供额外人工复核价值。
                """.formatted(scenario.id(), scenario.role(), scenario.jobRequirements(), scenario.resumeText(), ruleOnly);
    }

    private List<String> contributionChecks(DeepSeekChatResult model, ScoreEvidence evidence) {
        List<String> checks = new ArrayList<>();
        checks.add("规则分数来自本地技能覆盖率，模型结果未写回分数。");
        checks.add("缺口检查：" + (mentionsAny(model.content(), evidence.missing()) ? "模型文本提到至少一个缺口。"
                : "模型文本未覆盖缺口或本次未生成。"));
        checks.add("人工复核检查：" + (containsAny(model.content(), List.of("人工", "复核", "核验", "review"))
                ? "模型文本提示人工核验。" : "模型文本未明确提示人工核验或本次未生成。"));
        if (!model.generated()) {
            checks.add("降级检查：模型未生成时仍返回规则基线和缺口。");
        }
        return checks;
    }

    private List<String> riskFlags(DeepSeekChatResult model, ScoreEvidence evidence) {
        List<String> risks = new ArrayList<>();
        String text = safe(model.content()).toLowerCase(Locale.ROOT);
        if (!model.generated()) {
            risks.add("模型未生成，不能评估额外贡献。");
        }
        if (containsAny(text, List.of("直接录用", "直接拒绝", "自动录用", "自动拒绝", "offer"))) {
            risks.add("模型文本可能越过人工招聘决定边界。");
        }
        if (containsAny(text, List.of("修改分数", "调整分数", "改为100", "改成100"))) {
            risks.add("模型文本可能尝试改写规则分数。");
        }
        if (!evidence.missing().isEmpty() && model.generated() && !mentionsAny(model.content(), evidence.missing())) {
            risks.add("模型文本遗漏规则缺口，需人工复核。");
        }
        if (risks.isEmpty()) {
            risks.add("未发现自动决策或改分表述；仍需人工核验真实性。");
        }
        return risks;
    }

    private boolean mentionsAny(String text, Set<String> values) {
        if (values.isEmpty()) return true;
        String haystack = safe(text).toLowerCase(Locale.ROOT);
        return values.stream().anyMatch(value -> haystack.contains(value.toLowerCase(Locale.ROOT)));
    }

    private boolean containsAny(String text, List<String> needles) {
        String haystack = safe(text);
        return needles.stream().anyMatch(haystack::contains);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private List<Scenario> scenarios() {
        return List.of(
                new Scenario("student-gap-plan", "STUDENT",
                        "English, lesson planning, classroom management",
                        "英语教学设计与备课经验，参与课后答疑；尚未写出班级秩序维护案例。"),
                new Scenario("hr-risk-review", "HR",
                        "Java, Spring Boot, Redis, English",
                        "Java Spring Boot 项目经历，英文助教沟通；尚未写出缓存压测证据。"),
                new Scenario("high-score-human-check", "HR",
                        "Java, Spring Boot, MySQL",
                        "Java / Spring Boot 后端项目，MySQL 表设计与接口联调经验。"));
    }

    private record Scenario(String id, String role, String jobRequirements, String resumeText) {
    }

    private record ScoreEvidence(Set<String> required, Set<String> matched, Set<String> missing,
                                 BigDecimal score) {
    }
}
