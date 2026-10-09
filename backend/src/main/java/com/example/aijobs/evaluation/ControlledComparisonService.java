package com.example.aijobs.evaluation;

import com.example.aijobs.deepseek.DeepSeekChatClient;
import com.example.aijobs.deepseek.DeepSeekChatResult;
import com.example.aijobs.deepseek.DeepSeekMessage;
import com.example.aijobs.match.SkillNormalizer;
import com.example.aijobs.match.SkillCoverageService;
import com.example.aijobs.match.SkillCoverageService.Evidence;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class ControlledComparisonService {
    static final String SCENARIO_SET_VERSION = "day30-synthetic-v2";

    private final DeepSeekChatClient deepSeekChatClient;
    private final SkillCoverageService skillCoverageService;

    public ControlledComparisonService(DeepSeekChatClient deepSeekChatClient,
                                       SkillCoverageService skillCoverageService) {
        this.deepSeekChatClient = deepSeekChatClient;
        this.skillCoverageService = skillCoverageService;
    }

    public ControlledComparisonReport runDay30Comparison() {
        List<Scenario> scenarios = scenarios();
        List<ControlledComparisonResult> results = scenarios.stream()
                .map(this::runScenario)
                .toList();
        long generated = results.stream().filter(ControlledComparisonResult::modelGenerated).count();
        long fallback = results.size() - generated;
        String summary = "固定合成输入完成规则基线与规则加模型辅助对照；规则分数始终由本地算法给出，模型文本只作为人工复核材料。";
        if (generated == 0) {
            summary += " 本次外部模型未生成文本，只验证降级链路和评估记录结构。";
        } else if (fallback > 0) {
            summary += " 本次仅部分场景返回辅助文本，其余场景降级；不能视为完整模型比较。";
        }
        String runMode = generated == 0 ? "rule-plus-fallback"
                : fallback == 0 ? "rule-plus-model-output" : "rule-plus-partial-output";
        return new ControlledComparisonReport(
                "day30-controlled-comparison-2026-10-06",
                SCENARIO_SET_VERSION,
                scenarioSetHash(scenarios),
                runMode,
                SkillCoverageService.MODEL_NAME,
                SkillNormalizer.VERSION,
                results.size(),
                Math.toIntExact(generated),
                Math.toIntExact(fallback),
                true,
                "PENDING_HUMAN_REVIEW",
                "系统已生成固定场景的规则证据、模型文本状态和风险提示；尚未由人工完成质量复核或给出模型额外贡献结论。",
                summary,
                results,
                List.of(
                        "scenarioSetHash 由固定合成场景的 ID、角色、岗位要求和简历文本计算，用于确认输入集合未变化。",
                        "inputHash、ruleEvidenceHash 和 modelPromptHash 分别标识单场景输入、规则证据和发送给模型的提示词。",
                        "latencyMs、模型文本和 token 用量可能随运行变化，不应用作复现同一输入与规则口径的主键。"),
                List.of(
                        "全部场景均为合成招聘数据，不代表真实候选人或真实岗位效果。",
                        "运行模式只表示适配器返回文本的情况；模拟响应也可生成文本，真实调用须另有执行证据。",
                        "文本关键词检查不能判断建议的真实性、完整性或实际额外贡献，仍需人工审查。",
                        "模型输出不参与分数计算，不自动修改投递状态，也不形成录用或拒绝决定。",
                        "缺少 DeepSeek 凭证或开关关闭时，只能验证降级行为，不能声称完成真实模型增益评估。",
                        "即使启用真实模型，本接口也只报告小样本检查结果，不能外推为招聘准确率或效率提升。"));
    }

    private ControlledComparisonResult runScenario(Scenario scenario) {
        Evidence evidence = skillCoverageService.calculate(scenario.resumeText(), scenario.jobRequirements());
        String ruleOnly = ruleOnlySummary(evidence);
        String systemPrompt = systemPrompt();
        String userPrompt = userPrompt(scenario, ruleOnly);
        List<DeepSeekMessage> messages = List.of(
                new DeepSeekMessage("system", systemPrompt),
                new DeepSeekMessage("user", userPrompt));
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
                scenario.jobRequirements(),
                scenario.resumeText(),
                hash(canonicalScenario(scenario)),
                hash(canonicalRuleEvidence(evidence)),
                hash("system\n" + systemPrompt + "\nuser\n" + userPrompt),
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
                "PENDING_HUMAN_REVIEW",
                manualReviewChecklist(scenario, model, evidence),
                checks,
                risks);
    }

    private String ruleOnlySummary(Evidence evidence) {
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

    private String userPrompt(Scenario scenario, String ruleOnly) {
        return """
                场景：%s
                角色：%s
                岗位要求：%s
                简历文本：%s
                %s
                请在不改变分数的前提下，说明规则加模型相对规则基线能否提供额外人工复核价值。
                """.formatted(scenario.id(), scenario.role(), scenario.jobRequirements(), scenario.resumeText(), ruleOnly);
    }

    private List<String> contributionChecks(DeepSeekChatResult model, Evidence evidence) {
        List<String> checks = new ArrayList<>();
        checks.add("规则分数来自本地技能覆盖率，模型结果未写回分数。");
        if (evidence.missing().isEmpty()) {
            checks.add("缺口检查：规则未识别到缺口，不适用文本缺口覆盖检查。");
        } else {
            checks.add("缺口检查：" + (model.generated() && mentionsAny(model.content(), evidence.missing())
                    ? "模型文本提到至少一个缺口。" : "模型文本未覆盖缺口或本次未生成。"));
        }
        checks.add("人工复核检查：" + (containsAny(model.content(), List.of("人工", "复核", "核验", "review"))
                ? "模型文本提示人工核验。" : "模型文本未明确提示人工核验或本次未生成。"));
        if (!model.generated()) {
            checks.add("降级检查：模型未生成时仍返回规则基线和缺口。");
        }
        return checks;
    }

    private List<String> manualReviewChecklist(Scenario scenario, DeepSeekChatResult model, Evidence evidence) {
        List<String> checklist = new ArrayList<>();
        checklist.add("核对场景 " + scenario.id() + " 的岗位要求、简历文本、规则分、匹配技能和缺口是否与合成输入一致。");
        if (model.generated()) {
            checklist.add(evidence.missing().isEmpty()
                    ? "检查模型文本是否夸大熟练度或引入无依据经历，不把规则全覆盖视为真实性证明。"
                    : "检查模型文本是否准确覆盖规则缺口，并标注没有覆盖、夸大或引入无依据经历的部分。");
            checklist.add("评估模型文本相对规则基线是否提供了可执行的人工追问、风险提示或证据核验动作。");
        } else {
            checklist.add("记录模型未生成文本，本场景不能评估模型额外贡献，只能复核规则基线和降级说明。");
        }
        if (evidence.missing().isEmpty()) {
            checklist.add("规则未识别词典内技能缺口时，人工复核应重点检查熟练度、真实性和岗位语境，而不是寻找缺口覆盖。");
        } else {
            checklist.add("逐项核验缺口：" + String.join("、", evidence.missing()) + "，确认模型或人工备注没有把缺口当作已满足技能。");
        }
        checklist.add("确认模型文本未建议自动录用/拒绝、未改写规则分数，最终招聘决定仍由人工复核。");
        return checklist;
    }

    private List<String> riskFlags(DeepSeekChatResult model, Evidence evidence) {
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

    private boolean mentionsAny(String text, List<String> values) {
        String haystack = safe(text).toLowerCase(Locale.ROOT);
        return values.stream().anyMatch(value -> haystack.contains(value.toLowerCase(Locale.ROOT)));
    }

    private boolean containsAny(String text, List<String> needles) {
        String haystack = safe(text);
        return needles.stream().anyMatch(haystack::contains);
    }

    private String scenarioSetHash(List<Scenario> scenarios) {
        return hash(String.join("\n---\n", scenarios.stream().map(this::canonicalScenario).toList()));
    }

    private String canonicalScenario(Scenario scenario) {
        return String.join("\n",
                "id=" + scenario.id(),
                "role=" + scenario.role(),
                "jobRequirements=" + scenario.jobRequirements(),
                "resumeText=" + scenario.resumeText());
    }

    private String canonicalRuleEvidence(Evidence evidence) {
        return String.join("\n",
                "model=" + SkillCoverageService.MODEL_NAME,
                "dictionary=" + SkillNormalizer.VERSION,
                "score=" + evidence.score().toPlainString(),
                "required=" + String.join("|", evidence.required()),
                "matched=" + String.join("|", evidence.matched()),
                "missing=" + String.join("|", evidence.missing()));
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
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

}
