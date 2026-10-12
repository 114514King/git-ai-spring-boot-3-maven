package com.example.aijobs.evaluation;

import com.example.aijobs.deepseek.DeepSeekChatClient;
import com.example.aijobs.deepseek.DeepSeekChatResult;
import com.example.aijobs.deepseek.DeepSeekMessage;
import com.example.aijobs.deepseek.DeepSeekProperties;
import com.example.aijobs.match.SkillCoverageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in live transport and rule-isolation check, not a model-quality approval. */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_LIVE_TEST", matches = "true")
class ControlledComparisonLiveIT {
    private static final String OFFICIAL_ENDPOINT = "https://api.deepseek.com/chat/completions";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);

    @Test
    void comparesFixedSyntheticRulesWithThreeOfficialApiResponses() throws Exception {
        String key = System.getenv("DEEPSEEK_API_KEY");
        assertTrue(key != null && !key.isBlank(),
                "DEEPSEEK_API_KEY is required in the runtime environment; never put it in source files");
        key = key.strip();
        String model = System.getenv().getOrDefault("DEEPSEEK_MODEL", "deepseek-flash");
        Duration requestTimeout = Duration.parse(System.getenv().getOrDefault("DEEPSEEK_REQUEST_TIMEOUT", "PT15S"));
        int maxTokens = Integer.parseInt(System.getenv().getOrDefault("DEEPSEEK_MAX_TOKENS", "512"));
        String sourceCommit = System.getenv().getOrDefault("EVALUATION_SOURCE_COMMIT", "not-provided");
        assertTrue(sourceCommit.equals("not-provided") || sourceCommit.matches("[0-9a-fA-F]{40,64}"),
                "EVALUATION_SOURCE_COMMIT must be a full Git commit hash when supplied");

        Instant startedAt = Instant.now();
        ObjectMapper mapper = new ObjectMapper();
        ControlledComparisonReport baseline;
        ControlledComparisonReport live;
        List<RecordedCall> calls;
        try (DeepSeekChatClient disabled = new DeepSeekChatClient(
                new DeepSeekProperties(false, "", model, CONNECT_TIMEOUT, requestTimeout, maxTokens), mapper)) {
            baseline = new ControlledComparisonService(disabled, new SkillCoverageService()).runDay30Comparison();
        }
        // The production constructor fixes the official HTTPS endpoint and disables redirects/retries.
        // One unchanged service execution makes exactly three outbound synthetic requests.
        try (RecordingClient client = new RecordingClient(
                new DeepSeekProperties(true, key, model, CONNECT_TIMEOUT, requestTimeout, maxTokens), mapper)) {
            live = new ControlledComparisonService(client, new SkillCoverageService()).runDay30Comparison();
            calls = List.copyOf(client.calls);
        }

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("evidenceVersion", "day35-live-synthetic-v1");
        evidence.put("executionSource", "official-deepseek-api-via-production-client");
        evidence.put("startedAtUtc", startedAt.toString());
        evidence.put("completedAtUtc", Instant.now().toString());
        evidence.put("sourceCommit", sourceCommit);
        evidence.put("executionPath", "Direct production comparison service; no Spring HTTP or database execution");
        evidence.put("configuration", Map.of(
                "enabled", true, "endpoint", OFFICIAL_ENDPOINT, "requestedModel", model,
                "connectTimeout", CONNECT_TIMEOUT.toString(), "requestTimeout", requestTimeout.toString(),
                "maxTokens", maxTokens, "stream", false, "thinking", "disabled"));
        evidence.put("baselineSource", "Production client explicitly disabled; zero external requests");
        evidence.put("baseline", baseline);
        evidence.put("liveReport", live);
        evidence.put("adapterCalls", calls);
        evidence.put("limitations", List.of(
                "Only fixed synthetic inputs were sent; no candidate, database, or authentication data was read.",
                "This check validates live transport and unchanged rule evidence, not model quality or extra benefit.",
                "A length finish reason is recorded as truncated; generation does not imply complete advice.",
                "Human quality review remains pending; no hiring action or score update is performed.",
                "Raw evidence stays under ignored target/live-evaluation and must not be committed."));

        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(evidence);
        assertFalse(json.contains(key), "Refusing to persist evidence containing the runtime API key");
        Path directory = Path.of(System.getProperty("basedir", "."), "target", "live-evaluation")
                .toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String filename = "day35-" + startedAt.toEpochMilli() + "-" + UUID.randomUUID() + ".json";
        Path output = directory.resolve(filename);
        // Preserve safe response evidence even when a live-status or comparison assertion below fails.
        Files.writeString(output, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        System.out.println("LIVE_EVALUATION_EVIDENCE=" + output);
        System.out.println("LIVE_EVALUATION_STATUS generated=" + live.generatedCount()
                + "/" + live.scenarioCount() + " fallback=" + live.fallbackCount()
                + " truncated=" + calls.stream().filter(RecordedCall::truncated).count()
                + " qualityReview=PENDING_HUMAN_REVIEW");

        assertAll("Live comparison transport and rule isolation",
                () -> assertEquals(3, calls.size(), "Expected exactly three real adapter invocations"),
                () -> assertEquals(3, baseline.scenarioCount()),
                () -> assertEquals(0, baseline.generatedCount()),
                () -> assertEquals(3, baseline.fallbackCount()),
                () -> assertEquals("rule-plus-fallback", baseline.runMode()),
                () -> assertEquals(3, live.scenarioCount()),
                () -> assertEquals(3, live.generatedCount(), "Inspect saved adapter statuses for any fallback"),
                () -> assertEquals(0, live.fallbackCount()),
                () -> assertEquals("rule-plus-model-output", live.runMode()),
                () -> assertEquals(baseline.scenarioSetVersion(), live.scenarioSetVersion()),
                () -> assertEquals(baseline.scenarioSetHash(), live.scenarioSetHash()),
                () -> assertEquals(baseline.ruleModelName(), live.ruleModelName()),
                () -> assertEquals(baseline.dictionaryVersion(), live.dictionaryVersion()),
                () -> assertTrue(live.humanReviewRequired()),
                () -> assertEquals("PENDING_HUMAN_REVIEW", live.qualityReviewStatus()),
                () -> assertEquals(List.of("student-gap-plan", "hr-risk-review", "high-score-human-check"),
                        live.results().stream().map(ControlledComparisonResult::scenarioId).toList()),
                () -> assertEquals(List.of(new BigDecimal("66.67"), new BigDecimal("75.00"), new BigDecimal("100.00")),
                        live.results().stream().map(ControlledComparisonResult::ruleScore).toList()),
                () -> assertEquals(List.of(List.of("Classroom management"), List.of("Redis"), List.of()),
                        live.results().stream().map(ControlledComparisonResult::missingSkills).toList()),
                () -> verifyEachScenario(baseline, live, calls));
    }

    private void verifyEachScenario(ControlledComparisonReport baseline, ControlledComparisonReport live,
                                    List<RecordedCall> calls) {
        assertEquals(baseline.results().size(), live.results().size());
        assertEquals(live.results().size(), calls.size());
        for (int i = 0; i < live.results().size(); i++) {
            ControlledComparisonResult before = baseline.results().get(i);
            ControlledComparisonResult after = live.results().get(i);
            RecordedCall call = calls.get(i);
            assertAll(after.scenarioId(),
                    () -> assertEquals(DeepSeekChatResult.Status.DISABLED, before.modelStatus()),
                    () -> assertEquals(before.scenarioId(), after.scenarioId()),
                    () -> assertEquals(before.jobRequirements(), after.jobRequirements()),
                    () -> assertEquals(before.resumeText(), after.resumeText()),
                    () -> assertEquals(before.inputHash(), after.inputHash()),
                    () -> assertEquals(before.ruleEvidenceHash(), after.ruleEvidenceHash()),
                    () -> assertEquals(before.modelPromptHash(), after.modelPromptHash()),
                    () -> assertEquals(before.ruleScore(), after.ruleScore()),
                    () -> assertEquals(before.requiredSkills(), after.requiredSkills()),
                    () -> assertEquals(before.matchedSkills(), after.matchedSkills()),
                    () -> assertEquals(before.missingSkills(), after.missingSkills()),
                    () -> assertEquals(before.ruleOnlySummary(), after.ruleOnlySummary()),
                    () -> assertEquals("PENDING_HUMAN_REVIEW", after.manualReviewStatus()),
                    () -> assertTrue(after.modelGenerated()),
                    () -> assertEquals(DeepSeekChatResult.Status.GENERATED, call.status()),
                    () -> assertEquals(Integer.valueOf(200), call.httpStatus()),
                    () -> assertTrue("stop".equals(call.finishReason()) || "length".equals(call.finishReason())),
                    () -> assertEquals("length".equals(call.finishReason()), call.truncated()),
                    () -> assertFalse(call.content().isBlank()),
                    () -> assertEquals(call.content(), after.rulePlusModelSummary()),
                    () -> assertFalse(call.returnedModel().isBlank()),
                    () -> assertEquals(call.returnedModel(), after.modelName()),
                    () -> assertTrue(after.latencyMs() >= 0),
                    () -> verifyUsage(call.usage(), after));
        }
    }

    private void verifyUsage(DeepSeekChatResult.Usage usage, ControlledComparisonResult result) {
        assertNotNull(usage, "The official API must report token usage for this live check");
        assertAll("Reported token accounting",
                () -> assertTrue(usage.promptTokens() > 0),
                () -> assertTrue(usage.completionTokens() > 0),
                () -> assertEquals((long) usage.promptTokens() + usage.completionTokens(), (long) usage.totalTokens()),
                () -> assertEquals(Integer.valueOf(usage.promptTokens()), result.promptTokens()),
                () -> assertEquals(Integer.valueOf(usage.completionTokens()), result.completionTokens()),
                () -> assertEquals(Integer.valueOf(usage.totalTokens()), result.totalTokens()));
    }

    private static final class RecordingClient extends DeepSeekChatClient {
        private final List<RecordedCall> calls = new ArrayList<>();

        private RecordingClient(DeepSeekProperties properties, ObjectMapper mapper) {
            super(properties, mapper);
        }

        @Override
        public DeepSeekChatResult generate(List<DeepSeekMessage> messages) {
            String startedAtUtc = Instant.now().toString();
            DeepSeekChatResult result = super.generate(messages);
            calls.add(new RecordedCall(startedAtUtc, Instant.now().toString(), List.copyOf(messages),
                    result.status(), result.model(), result.httpStatus(), result.finishReason(),
                    result.truncated(), result.usage(), result.content()));
            return result;
        }
    }

    private record RecordedCall(String startedAtUtc, String completedAtUtc, List<DeepSeekMessage> messages,
                                DeepSeekChatResult.Status status, String returnedModel, Integer httpStatus,
                                String finishReason, boolean truncated, DeepSeekChatResult.Usage usage,
                                String content) {
    }
}
