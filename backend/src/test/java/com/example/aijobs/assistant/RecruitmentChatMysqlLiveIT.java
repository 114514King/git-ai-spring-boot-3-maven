package com.example.aijobs.assistant;

import com.example.aijobs.AiJobPlatformApplication;
import com.example.aijobs.deepseek.DeepSeekChatClient;
import com.example.aijobs.deepseek.DeepSeekChatResult;
import com.example.aijobs.deepseek.DeepSeekMessage;
import com.example.aijobs.deepseek.DeepSeekProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;

/** Explicitly selected live test: real MySQL, production HTTP/auth/services and at most three official calls. */
@EnabledIfEnvironmentVariable(named = "ASSISTANT_MYSQL_LIVE_TEST", matches = "true")
@SpringBootTest(classes = AiJobPlatformApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.address=127.0.0.1", "app.jwt.secret=day37-isolated-http-test-secret-at-least-32-bytes",
                "app.jwt.expiration=PT2H", "app.deepseek.enabled=true", "app.deepseek.api-key=${DEEPSEEK_API_KEY:}",
                "app.deepseek.max-tokens=2048", "app.deepseek.request-timeout=PT30S", "app.assistant.max-history-messages=8",
                "app.external-jobs.enabled=false", "spring.sql.init.mode=never", "spring.data.redis.host=127.0.0.1",
                "spring.data.redis.timeout=200ms", "spring.data.redis.connect-timeout=200ms",
                "spring.data.redis.password=", "spring.data.redis.username=", "spring.data.redis.database=0"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RecruitmentChatMysqlLiveIT {
    private static final Pattern SAFE_DATABASE_URL = Pattern.compile(
            "jdbc:mysql://127\\.0\\.0\\.1:3306/(ai_job_platform_day37_[0-9]+)(?:\\?[^#]*)?");
    private static final Set<String> SAFE_CONNECTION_OPTIONS = Set.of("useUnicode=true", "characterEncoding=utf8",
            "serverTimezone=Asia/Shanghai", "allowPublicKeyRetrieval=true", "useSSL=false");
    private static final List<String> TABLES = List.of("ai_match_result", "job_application", "job_posting",
            "platform_user", "resume", "role", "user_role");
    private static final String FIRST = "SYNTHETIC_DAY37 student: Give one short Redis evidence-check question. Do not score or decide hiring.";
    private static final String FOLLOWUP = "SYNTHETIC_DAY37 follow-up: Use the previous question and give one short next step for human review.";
    private static final String HR = "SYNTHETIC_DAY37 HR: Give one short verification question about the Redis gap. A human decides hiring.";
    private static final String SOURCE_TEXT = "SYNTHETIC_DAY37 fictional exercise only: Java, Spring Boot and MySQL practice.";
    private static final String STUDENT_PATH = "/api/student/assistant/chat";
    private static final String HR_PATH = "/api/hr/assistant/chat";
    private static ServerSocket unavailableRedisPort;
    private static String databaseName;

    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired RedisConnectionFactory redisConnectionFactory;
    @Autowired DeepSeekProperties properties;
    @SpyBean DeepSeekChatClient adapter;
    private final List<HttpObservation> http = new ArrayList<>();
    private final List<RecordedCall> calls = Collections.synchronizedList(new ArrayList<>());
    private final List<String> credentials = new ArrayList<>();
    private final AtomicInteger attempts = new AtomicInteger();
    private final Instant startedAt = Instant.now();
    private final String accountPassword = "Synthetic37-" + UUID.randomUUID();
    private Map<String, JsonNode> before;
    private Map<String, JsonNode> after;
    private String mysqlVersion;
    private long jobId;
    private long resumeId;
    private long applicationId;
    private long matchId;
    private String studentToken;
    private String otherStudentToken;
    private String hrToken;
    private String otherHrToken;

    @DynamicPropertySource
    static void isolatedResources(DynamicPropertyRegistry registry) throws Exception {
        // Validate before Spring creates its datasource; never allow a default or existing business schema.
        String url = System.getenv("DB_URL");
        var matcher = SAFE_DATABASE_URL.matcher(url == null ? "" : url);
        if (!matcher.matches()) throw new IllegalStateException("DB_URL must identify a loopback ai_job_platform_day37_<digits> schema");
        int query = url.indexOf('?');
        if (query >= 0) {
            List<String> options = List.of(url.substring(query + 1).split("&", -1));
            if (!SAFE_CONNECTION_OPTIONS.containsAll(options) || Set.copyOf(options).size() != options.size()) {
                throw new IllegalStateException("DB_URL contains unsupported or repeated connection options");
            }
        }
        if (System.getenv("DB_PASSWORD") == null || System.getenv("DB_PASSWORD").isBlank()) {
            throw new IllegalStateException("A runtime DB_PASSWORD is required for the isolated live check");
        }
        if (System.getenv("DEEPSEEK_API_KEY") == null || System.getenv("DEEPSEEK_API_KEY").isBlank()) {
            throw new IllegalStateException("A runtime DEEPSEEK_API_KEY is required for the explicit live check");
        }
        databaseName = matcher.group(1);
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("DB_USERNAME", "root"));
        registry.add("spring.datasource.password", () -> System.getenv("DB_PASSWORD"));
        // Reserve a loopback TCP port without providing Redis. Production cache timeout/fallback executes;
        // no cache mock is installed, and existing Redis keys cannot be invalidated by fixture creation.
        unavailableRedisPort = new ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"));
        registry.add("spring.data.redis.port", unavailableRedisPort::getLocalPort);
        registry.add("spring.data.redis.url", () -> "redis://127.0.0.1:" + unavailableRedisPort.getLocalPort());
    }

    @AfterAll
    static void releaseReservedPort() throws Exception {
        if (unavailableRedisPort != null) unavailableRedisPort.close();
    }

    @Test
    void realMysqlAuthRuleMatchAndOfficialAssistantPreserveStoredBusinessData() throws Exception {
        LettuceConnectionFactory redis = assertInstanceOf(LettuceConnectionFactory.class, redisConnectionFactory);
        assertNull(redis.getClusterConfiguration(), "The isolated check must not use a Redis cluster");
        assertNull(redis.getSentinelConfiguration(), "The isolated check must not use Redis Sentinel");
        assertEquals("127.0.0.1", redis.getHostName());
        assertEquals(unavailableRedisPort.getLocalPort(), redis.getPort());
        assertEquals(databaseName, jdbc.queryForObject("SELECT DATABASE()", String.class));
        mysqlVersion = jdbc.queryForObject("SELECT VERSION()", String.class);
        assertTrue(mysqlVersion != null && mysqlVersion.startsWith("8."), "This acceptance targets MySQL 8");
        assertEquals(TABLES, jdbc.queryForList("SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE' ORDER BY table_name", String.class));
        for (String table : TABLES) {
            if (!table.equals("role")) {
                assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class),
                        "Refusing to use a nonempty isolated business table: " + table);
            }
        }
        assertEquals(List.of("ADMIN", "HR", "STUDENT"), jdbc.queryForList("SELECT code FROM role ORDER BY code", String.class));
        credentials.add(properties.apiKey());
        credentials.add(System.getenv("DB_PASSWORD"));
        credentials.add(accountPassword);
        observeRealAdapter();
        try {
            createSyntheticFixturesThroughHttp();
            assertEquals(0, attempts.get(), "Fixture creation and rule scoring must never call the model");
            before = snapshot();
            exerciseAssistant();
            after = snapshot();
            assertTrue(before.equals(after), "Every row and column in all seven tables must remain unchanged during chat");
            assertPersistedRuleAndApplication();
            assertEquals(3, attempts.get());
            assertEquals(3, calls.size());
            for (RecordedCall call : calls) assertCompleteOfficialResponse(call.result());
        } finally {
            if (before != null && after == null) after = snapshot();
            saveEvidence();
        }
        System.out.println("LIVE_ASSISTANT_MYSQL_STATUS generated=3/3 persistence=REAL_MYSQL humanQuality=UNVERIFIED redis=PRODUCTION_FALLBACK");
    }

    private void observeRealAdapter() {
        // Observe the actual client, retaining all transport, parsing and fallback behavior. Never stub a response.
        doAnswer(invocation -> {
            List<DeepSeekMessage> messages = invocation.getArgument(0);
            assertTrue(messages.size() == 3 || messages.size() == 5);
            assertEquals("system", messages.getFirst().role());
            assertTrue(messages.get(1).content().contains("SYNTHETIC_DAY37 fictional CV"));
            assertTrue(messages.get(1).content().contains(SOURCE_TEXT));
            assertTrue(messages.get(1).content().contains("只读匹配分：75.00"));
            assertTrue(Set.of(FIRST, FOLLOWUP, HR).contains(messages.getLast().content()), "Only fixed synthetic questions may leave the process");
            int attempt = attempts.incrementAndGet();
            assertTrue(attempt <= 3, "Hard limit: at most three official model attempts, with no retry");
            String start = Instant.now().toString();
            DeepSeekChatResult result = (DeepSeekChatResult) invocation.callRealMethod();
            calls.add(new RecordedCall(start, Instant.now().toString(), List.copyOf(messages), result));
            return result;
        }).when(adapter).generate(anyList());
    }

    private void createSyntheticFixturesThroughHttp() throws Exception {
        studentToken = registerAndLogin("student", "STUDENT");
        otherStudentToken = registerAndLogin("other_student", "STUDENT");
        hrToken = registerAndLogin("hr", "HR");
        otherHrToken = registerAndLogin("other_hr", "HR");
        jobId = request("create-job", "POST", "/api/hr/jobs", hrToken, Map.of(
                "title", "SYNTHETIC_DAY37 teaching assistant", "companyName", "SYNTHETIC_DAY37 fictional school",
                "city", "Synthetic city", "employmentType", "PART_TIME", "description", "SYNTHETIC_DAY37 teaching support exercise",
                "requirements", "Java, Spring Boot, MySQL, Redis"), 201).path("data").path("id").asLong();
        assertTrue(jobId > 0);
        request("publish-job", "PATCH", "/api/hr/jobs/" + jobId + "/status", hrToken, Map.of("status", "PUBLISHED"), 200);
        resumeId = request("create-resume", "POST", "/api/student/resumes", studentToken, Map.of(
                "title", "SYNTHETIC_DAY37 fictional CV", "skills", "Java, Spring Boot", "sourceText", SOURCE_TEXT),
                201).path("data").path("id").asLong();
        assertTrue(resumeId > 0);
        request("publish-resume", "PATCH", "/api/student/resumes/" + resumeId + "/status", studentToken, Map.of("status", "PUBLISHED"), 200);
        applicationId = request("submit-application", "POST", "/api/student/applications", studentToken,
                Map.of("jobId", jobId, "resumeId", resumeId), 201).path("data").path("id").asLong();
        assertTrue(applicationId > 0);
        JsonNode match = request("compute-rule-match", "POST", "/api/student/matches", studentToken,
                Map.of("jobId", jobId, "resumeId", resumeId), 200).path("data");
        matchId = match.path("id").asLong();
        assertTrue(matchId > 0);
        assertEquals(0, new BigDecimal("75.00").compareTo(match.path("score").decimalValue()));
        assertEquals("简历文本尚未识别的岗位技能：Redis；未提及不等于不具备。", match.path("gapSummary").asText());
        assertEquals(SOURCE_TEXT, jdbc.queryForObject("SELECT source_text FROM resume WHERE id = ?", String.class, resumeId));
        assertPersistedRuleAndApplication();
    }

    private String registerAndLogin(String suffix, String role) throws Exception {
        String username = "synthetic_day37_" + suffix;
        request("register-" + suffix, "POST", "/api/auth/register", null,
                Map.of("username", username, "password", accountPassword, "email", username + "@example.invalid", "role", role), 201);
        JsonNode login = request("login-" + suffix, "POST", "/api/auth/login", null,
                Map.of("account", username, "password", accountPassword), 200).path("data");
        String token = login.path("accessToken").asText();
        assertFalse(token.isBlank(), "Login must return an accessToken");
        credentials.add(token);
        assertTrue(login.path("roles").toString().contains(role));
        return token;
    }

    private void exerciseAssistant() throws Exception {
        denied("anonymous", STUDENT_PATH, null, Map.of("message", FIRST), 401);
        denied("student-wrong-role", HR_PATH, studentToken, Map.of("message", HR), 403);
        denied("hr-wrong-role", STUDENT_PATH, hrToken, Map.of("message", FIRST), 403);
        denied("foreign-student-application", STUDENT_PATH, otherStudentToken, chatRequest(FIRST), 403);
        denied("foreign-student-resume", STUDENT_PATH, otherStudentToken, Map.of("message", FIRST, "resumeId", resumeId), 403);
        denied("foreign-hr-job", HR_PATH, otherHrToken, Map.of("message", HR, "jobId", jobId), 403);
        denied("foreign-hr-application", HR_PATH, otherHrToken, chatRequest(HR), 403);
        denied("message-limit", STUDENT_PATH, studentToken, Map.of("message", "a".repeat(1001)), 400);
        JsonNode first = request("student-first", "POST", STUDENT_PATH, studentToken, chatRequest(FIRST), 200).path("data");
        checkChatResponse(first, "STUDENT", 2);
        assertCompleteOfficialResponse(calls.getLast().result());
        String conversation = first.path("conversationId").asText();
        assertTrue(conversation.startsWith("chat_"));
        denied("foreign-conversation", STUDENT_PATH, otherStudentToken, Map.of("message", FIRST, "conversationId", conversation), 403);
        denied("changed-context", STUDENT_PATH, studentToken, Map.of("message", FIRST, "conversationId", conversation), 409);
        JsonNode followup = request("student-followup", "POST", STUDENT_PATH, studentToken,
                Map.of("message", FOLLOWUP, "applicationId", applicationId, "conversationId", conversation), 200).path("data");
        checkChatResponse(followup, "STUDENT", 4);
        assertCompleteOfficialResponse(calls.getLast().result());
        assertEquals(conversation, followup.path("conversationId").asText());
        JsonNode hr = request("hr-owned-application", "POST", HR_PATH, hrToken, chatRequest(HR), 200).path("data");
        checkChatResponse(hr, "HR", 2);
        assertCompleteOfficialResponse(calls.getLast().result());
        assertNotEquals(conversation, hr.path("conversationId").asText());
        assertEquals(3, calls.size());
        List<DeepSeekMessage> original = calls.get(0).messages();
        List<DeepSeekMessage> continued = calls.get(1).messages();
        assertEquals(3, original.size());
        assertEquals(5, continued.size());
        assertEquals(original.get(2), continued.get(2));
        assertEquals("assistant", continued.get(3).role());
        assertEquals(first.path("assistantMessage").asText(), continued.get(3).content());
        assertEquals(FOLLOWUP, continued.get(4).content());
        assertTrue(original.getFirst().content().contains("学生求职准备"));
        assertTrue(calls.get(2).messages().getFirst().content().contains("HR 招聘复核"));
    }

    private Map<String, Object> chatRequest(String message) { return Map.of("message", message, "applicationId", applicationId); }

    private void checkChatResponse(JsonNode data, String role, int historyCount) {
        assertEquals(role, data.path("role").asText());
        assertEquals(historyCount, data.path("historyMessageCount").asInt());
        assertEquals(0, new BigDecimal("75.00").compareTo(data.path("readOnlyMatchScore").decimalValue()));
        assertEquals("ai-match-result", data.path("matchScoreSource").asText());
        assertTrue(data.path("humanReviewRequired").asBoolean());
        assertTrue(data.path("contextSummary").asText().contains("SUBMITTED"));
        assertTrue(data.path("generated").asBoolean(), "A fallback response is not a successful live acceptance");
        assertEquals("GENERATED", data.path("modelStatus").asText());
        assertFalse(data.path("assistantMessage").asText().isBlank());
    }

    private void assertPersistedRuleAndApplication() {
        assertEquals(0, new BigDecimal("75.00").compareTo(jdbc.queryForObject("SELECT score FROM ai_match_result WHERE id = ?", BigDecimal.class, matchId)));
        assertEquals("local-skill-coverage-v1", jdbc.queryForObject("SELECT model_name FROM ai_match_result WHERE id = ?", String.class, matchId));
        assertEquals("简历文本尚未识别的岗位技能：Redis；未提及不等于不具备。", jdbc.queryForObject("SELECT gap_summary FROM ai_match_result WHERE id = ?", String.class, matchId));
        assertEquals("SUBMITTED", jdbc.queryForObject("SELECT status FROM job_application WHERE id = ?", String.class, applicationId));
    }

    private void denied(String label, String path, String token, Map<String, ?> body, int status) throws Exception {
        int previous = attempts.get();
        request(label, "POST", path, token, body, status);
        assertEquals(previous, attempts.get(), label + " must be rejected before the adapter");
    }

    private JsonNode request(String label, String method, String path, String token, Map<String, ?> body, int expected) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(40)).header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body), StandardCharsets.UTF_8));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()) {
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode parsed = json.readTree(response.body());
            // Store no fixture/auth response or request body: login tokens and account passwords never enter the evidence.
            http.add(new HttpObservation(label, method, path, response.statusCode(), parsed.path("success").asBoolean(),
                    path.endsWith("/assistant/chat") ? parsed : null));
            assertEquals(expected, response.statusCode(), label);
            assertEquals(expected >= 200 && expected < 300, parsed.path("success").asBoolean(), label);
            return parsed;
        }
    }

    private Map<String, JsonNode> snapshot() {
        Map<String, JsonNode> snapshot = new LinkedHashMap<>();
        for (String table : TABLES) snapshot.put(table, json.valueToTree(jdbc.queryForList("SELECT * FROM " + table + " ORDER BY id")));
        return snapshot;
    }

    private Map<String, Object> snapshotEvidence(Map<String, JsonNode> snapshot) throws Exception {
        if (snapshot == null) return Map.of("status", "not-reached");
        Map<String, Object> evidence = new LinkedHashMap<>();
        for (var entry : snapshot.entrySet()) evidence.put(entry.getKey(), Map.of("rowCount", entry.getValue().size(),
                "sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(entry.getValue())))));
        return evidence;
    }

    private void assertCompleteOfficialResponse(DeepSeekChatResult result) {
        assertEquals(DeepSeekChatResult.Status.GENERATED, result.status());
        assertEquals(Integer.valueOf(200), result.httpStatus());
        assertEquals("stop", result.finishReason(), "Truncated advice is not a complete live acceptance");
        assertFalse(result.truncated());
        assertFalse(result.content().isBlank());
        assertNotNull(result.usage());
        assertTrue(result.usage().promptTokens() > 0);
        assertTrue(result.usage().completionTokens() > 0);
        assertEquals((long) result.usage().promptTokens() + result.usage().completionTokens(), (long) result.usage().totalTokens());
    }

    private void saveEvidence() throws Exception {
        String sourceCommit = System.getenv().getOrDefault("EVALUATION_SOURCE_COMMIT", "not-provided");
        assertTrue(sourceCommit.equals("not-provided") || sourceCommit.matches("[0-9a-fA-F]{40,64}"), "Source commit must be a full Git SHA");
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("startedAtUtc", startedAt.toString());
        evidence.put("completedAtUtc", Instant.now().toString());
        evidence.put("sourceCommit", sourceCommit);
        evidence.put("executionPath", "Real loopback Spring HTTP/auth/JWT/MyBatis/MySQL/rules/chat/official DeepSeek; no business substitutes");
        evidence.put("database", Map.of("schema", databaseName, "version", mysqlVersion));
        evidence.put("configuration", Map.of("model", properties.model(), "maxTokens", properties.maxTokens(),
                "requestTimeout", properties.requestTimeout().toString(), "endpoint", "https://api.deepseek.com/chat/completions"));
        evidence.put("fixtureIds", Map.of("jobId", jobId, "resumeId", resumeId, "applicationId", applicationId, "matchId", matchId));
        evidence.put("ruleExpectation", Map.of("score", "75.00", "missingSkill", "Redis", "applicationStatus", "SUBMITTED", "sourceText", SOURCE_TEXT));
        evidence.put("databaseBeforeChat", snapshotEvidence(before));
        evidence.put("databaseAfterChat", snapshotEvidence(after));
        evidence.put("databaseUnchanged", before != null && before.equals(after));
        evidence.put("httpObservations", http);
        evidence.put("adapterAttemptCount", attempts.get());
        evidence.put("adapterCalls", List.copyOf(calls));
        evidence.put("limitations", List.of("Only fixed synthetic data in a new isolated database; no production migration or real CV acceptance.",
                "Redis intentionally unavailable on a reserved loopback port; existing production timeout/fallback executed, Redis success unverified.",
                "No browser exercised. Human quality review, measurable benefit and real collaboration history remain unverified."));
        String serialized = json.writerWithDefaultPrettyPrinter().writeValueAsString(evidence);
        for (String credential : credentials) {
            assertFalse(credential != null && !credential.isBlank() && serialized.contains(credential), "Refusing to persist a credential or authentication token");
        }
        Path directory = Path.of(System.getProperty("basedir", "."), "target", "live-assistant-mysql").toAbsolutePath().normalize();
        Files.createDirectories(directory);
        Path output = directory.resolve("day37-" + startedAt.toEpochMilli() + "-" + UUID.randomUUID() + ".json");
        Files.writeString(output, serialized, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        System.out.println("LIVE_ASSISTANT_MYSQL_EVIDENCE=" + output);
    }

    record HttpObservation(String label, String method, String path, int status, boolean success, JsonNode assistantResponse) { }
    record RecordedCall(String startedAtUtc, String completedAtUtc, List<DeepSeekMessage> messages, DeepSeekChatResult result) { }
}
