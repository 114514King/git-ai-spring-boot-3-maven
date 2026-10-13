package com.example.aijobs.assistant;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.aijobs.application.entity.JobApplication;
import com.example.aijobs.application.mapper.JobApplicationMapper;
import com.example.aijobs.auth.*;
import com.example.aijobs.auth.entity.PlatformUser;
import com.example.aijobs.auth.mapper.PlatformUserMapper;
import com.example.aijobs.auth.mapper.RoleMapper;
import com.example.aijobs.common.GlobalExceptionHandler;
import com.example.aijobs.deepseek.*;
import com.example.aijobs.job.entity.JobPosting;
import com.example.aijobs.job.mapper.JobPostingMapper;
import com.example.aijobs.match.entity.AiMatchResult;
import com.example.aijobs.match.mapper.AiMatchResultMapper;
import com.example.aijobs.resume.entity.Resume;
import com.example.aijobs.resume.mapper.ResumeMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Real loopback HTTP/security/chat/client; every persistence read is a fixed synthetic mock. */
@ActiveProfiles("assistant-http-harness")
abstract class RecruitmentChatHttpHarness {
    static final String FIRST = "SYNTHETIC_DAY36 student: Give one short Redis evidence-check question. Do not score or decide hiring.";
    static final String FOLLOWUP = "SYNTHETIC_DAY36 follow-up: Use the previous question and give one short next step for human review.";
    static final String HR = "SYNTHETIC_DAY36 HR: Give one short verification question about the Redis gap. A human decides hiring.";
    static final String STUDENT_PATH = "/api/student/assistant/chat";
    static final String HR_PATH = "/api/hr/assistant/chat";

    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    @Autowired JwtService jwt;
    @Autowired ApplicationContext context;
    @Autowired RecordingClient recorder;
    @Autowired DeepSeekProperties properties;
    @Autowired PlatformUserMapper users;
    @Autowired RoleMapper roles;
    @Autowired JobApplicationMapper applications;
    @Autowired JobPostingMapper jobs;
    @Autowired ResumeMapper resumes;
    @Autowired AiMatchResultMapper matches;
    final List<HttpObservation> http = new ArrayList<>();
    final List<String> tokens = new ArrayList<>();
    final Instant startedAt = Instant.now();
    List<Object> fixtures;
    JsonNode before;
    String studentToken;
    String otherStudentToken;
    String hrToken;
    String otherHrToken;

    @BeforeEach
    void syntheticFixtures() {
        assertTrue(context.getBeansOfType(DataSource.class).isEmpty(), "This harness must never connect to a database");
        reset(users, roles, applications, jobs, resumes, matches);
        recorder.calls.clear();
        recorder.attempts = 0;
        studentToken = actor(42L, "STUDENT");
        otherStudentToken = actor(43L, "STUDENT");
        hrToken = actor(7L, "HR");
        otherHrToken = actor(8L, "HR");

        JobPosting job = new JobPosting();
        job.setId(9L); job.setHrId(7L); job.setTitle("SYNTHETIC_DAY36 teaching assistant");
        job.setCompanyName("SYNTHETIC_DAY36 fictional school"); job.setCity("Synthetic city");
        job.setRequirements("Java, Spring Boot, MySQL, Redis"); job.setStatus("PUBLISHED");
        Resume resume = new Resume();
        resume.setId(5L); resume.setStudentId(42L); resume.setTitle("SYNTHETIC_DAY36 fictional CV");
        resume.setSkills("Java, Spring Boot, MySQL"); resume.setSourceText("Synthetic exercise only: Java, Spring Boot and MySQL practice.");
        resume.setStatus("PUBLISHED");
        JobApplication application = new JobApplication();
        application.setId(11L); application.setJobId(9L); application.setResumeId(5L);
        application.setStudentId(42L); application.setStatus("SUBMITTED");
        AiMatchResult match = new AiMatchResult();
        match.setId(13L); match.setJobId(9L); match.setResumeId(5L); match.setScore(new BigDecimal("75.00"));
        match.setModelName("local-skill-coverage-v1"); match.setStrengthSummary("Java, Spring Boot, MySQL");
        match.setGapSummary("Redis");
        when(jobs.selectById(9L)).thenReturn(job);
        when(resumes.selectById(5L)).thenReturn(resume);
        when(applications.selectById(11L)).thenReturn(application);
        // Resolve lambda column names without a SqlSession, connection, or database.
        if (TableInfoHelper.getTableInfo(AiMatchResult.class) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "day36-synthetic"), AiMatchResult.class);
        }
        when(matches.selectOne(any(Wrapper.class))).thenAnswer(invocation -> {
            LambdaQueryWrapper<?> query = assertInstanceOf(LambdaQueryWrapper.class, invocation.getArgument(0));
            var predicates = Pattern.compile("\\(job_id = #\\{ew\\.paramNameValuePairs\\.(\\w+)\\} AND resume_id = #\\{ew\\.paramNameValuePairs\\.(\\w+)\\}\\)")
                    .matcher(query.getSqlSegment());
            assertTrue(predicates.matches(), "Saved score must be selected by exactly the authorized job and resume");
            assertEquals(2, query.getParamNameValuePairs().size());
            assertEquals(9L, query.getParamNameValuePairs().get(predicates.group(1)));
            assertEquals(5L, query.getParamNameValuePairs().get(predicates.group(2)));
            return match;
        });
        fixtures = List.of(job, resume, application, match);
        before = json.valueToTree(fixtures);
    }

    private String actor(long id, String role) {
        PlatformUser user = new PlatformUser();
        user.setId(id); user.setUsername("SYNTHETIC_DAY36_" + id); user.setStatus("ACTIVE");
        when(users.selectById(id)).thenReturn(user);
        when(roles.selectCodesByUserId(id)).thenReturn(List.of(role));
        String token = jwt.issue(id, user.getUsername()).value();
        tokens.add(token);
        return token;
    }

    /** Exactly three authorized adapter invocations; all denied requests must stop before it. */
    void exerciseHttpFlow() throws Exception {
        denied("anonymous", STUDENT_PATH, null, Map.of("message", FIRST), 401);
        denied("student-wrong-role", HR_PATH, studentToken, Map.of("message", HR), 403);
        denied("hr-wrong-role", STUDENT_PATH, hrToken, Map.of("message", FIRST), 403);
        denied("foreign-student-application", STUDENT_PATH, otherStudentToken, request(FIRST), 403);
        denied("foreign-student-resume", STUDENT_PATH, otherStudentToken, Map.of("message", FIRST, "resumeId", 5), 403);
        denied("foreign-hr-job", HR_PATH, otherHrToken, Map.of("message", HR, "jobId", 9), 403);
        denied("foreign-hr-application", HR_PATH, otherHrToken, request(HR), 403);
        denied("message-limit", STUDENT_PATH, studentToken, Map.of("message", "a".repeat(1001)), 400);

        JsonNode first = post("student-first", STUDENT_PATH, studentToken, request(FIRST), 200).path("data");
        checkResponse(first, "STUDENT", 2);
        String conversation = first.path("conversationId").asText();
        assertTrue(conversation.startsWith("chat_"));
        denied("foreign-conversation", STUDENT_PATH, otherStudentToken,
                Map.of("message", FIRST, "conversationId", conversation), 403);
        denied("changed-context", STUDENT_PATH, studentToken,
                Map.of("message", FIRST, "conversationId", conversation), 409);
        JsonNode followup = post("student-followup", STUDENT_PATH, studentToken,
                Map.of("message", FOLLOWUP, "applicationId", 11, "conversationId", conversation), 200).path("data");
        checkResponse(followup, "STUDENT", 4);
        assertEquals(conversation, followup.path("conversationId").asText());
        JsonNode hr = post("hr-owned-application", HR_PATH, hrToken, request(HR), 200).path("data");
        checkResponse(hr, "HR", 2);
        assertNotEquals(conversation, hr.path("conversationId").asText());
        assertEquals(3, recorder.calls.size());
        List<DeepSeekMessage> firstMessages = recorder.calls.get(0).messages();
        List<DeepSeekMessage> continued = recorder.calls.get(1).messages();
        assertEquals(3, firstMessages.size());
        assertEquals(5, continued.size());
        assertEquals(firstMessages.get(2), continued.get(2));
        assertEquals("assistant", continued.get(3).role());
        assertEquals(first.path("assistantMessage").asText(), continued.get(3).content());
        assertEquals(FOLLOWUP, continued.get(4).content());
        assertTrue(firstMessages.getFirst().content().contains("学生求职准备"));
        assertTrue(recorder.calls.get(2).messages().getFirst().content().contains("HR 招聘复核"));
        assertReadOnly();
    }

    private Map<String, Object> request(String message) { return Map.of("message", message, "applicationId", 11); }

    private void checkResponse(JsonNode data, String role, int historyCount) {
        assertEquals(role, data.path("role").asText());
        assertEquals(historyCount, data.path("historyMessageCount").asInt());
        assertEquals(0, new BigDecimal("75.00").compareTo(data.path("readOnlyMatchScore").decimalValue()));
        assertEquals("ai-match-result", data.path("matchScoreSource").asText());
        assertTrue(data.path("humanReviewRequired").asBoolean());
        assertTrue(data.path("contextSummary").asText().contains("SUBMITTED"));
        assertFalse(data.path("assistantMessage").asText().isBlank());
    }

    private void denied(String label, String path, String token, Map<String, ?> body, int status) throws Exception {
        int callsBefore = recorder.attempts;
        post(label, path, token, body, status);
        assertEquals(callsBefore, recorder.attempts, label + " must not invoke the adapter");
    }

    private JsonNode post(String label, String path, String token, Map<String, ?> body, int status) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(40)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body), StandardCharsets.UTF_8));
        if (token != null) request.header("Authorization", "Bearer " + token);
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()) {
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode parsed = json.readTree(response.body());
            http.add(new HttpObservation(label, response.statusCode(), parsed));
            assertEquals(status, response.statusCode(), label);
            assertEquals(status == 200, parsed.path("success").asBoolean(), label);
            return parsed;
        }
    }

    void assertReadOnly() {
        assertEquals(before, json.valueToTree(fixtures), "No synthetic business entity may be mutated");
        for (Object mapper : List.of(users, roles, applications, jobs, resumes, matches)) {
            for (var invocation : mockingDetails(mapper).getInvocations()) {
                assertTrue(Set.of("selectById", "selectCodesByUserId", "selectOne").contains(invocation.getMethod().getName()),
                        "Unexpected mapper operation: " + invocation.getMethod().getName());
            }
        }
    }

    Path saveLiveEvidence() throws Exception {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("startedAtUtc", startedAt.toString());
        evidence.put("completedAtUtc", Instant.now().toString());
        evidence.put("executionPath", "Real loopback Spring HTTP/JWT/chat/official DeepSeek; fixed synthetic mocked persistence, no database");
        String sourceCommit = System.getenv().getOrDefault("EVALUATION_SOURCE_COMMIT", "not-provided");
        assertTrue(sourceCommit.equals("not-provided") || sourceCommit.matches("[0-9a-fA-F]{40,64}"),
                "EVALUATION_SOURCE_COMMIT must be a full Git commit hash when supplied");
        evidence.put("sourceCommit", sourceCommit);
        evidence.put("configuration", Map.of("model", properties.model(), "maxTokens", properties.maxTokens(),
                "requestTimeout", properties.requestTimeout().toString(), "endpoint", "https://api.deepseek.com/chat/completions"));
        evidence.put("fixturesBefore", before);
        evidence.put("fixturesAfter", json.valueToTree(fixtures));
        evidence.put("httpObservations", http);
        evidence.put("adapterCalls", recorder.calls);
        evidence.put("limitations", List.of("No real database or browser was exercised.",
                "The saved score is a fixed synthetic fixture, not a new rule calculation.",
                "Human quality review and measurable model benefit remain unverified."));
        String serialized = json.writerWithDefaultPrettyPrinter().writeValueAsString(evidence);
        assertFalse(properties.apiKey().isBlank(), "Live evidence requires a runtime API key");
        assertFalse(serialized.contains(properties.apiKey()), "Refusing to persist an API key");
        for (String token : tokens) assertFalse(serialized.contains(token), "Refusing to persist an authentication token");
        Path directory = Path.of(System.getProperty("basedir", "."), "target", "live-assistant").toAbsolutePath().normalize();
        Files.createDirectories(directory);
        Path output = directory.resolve("day36-" + startedAt.toEpochMilli() + "-" + UUID.randomUUID() + ".json");
        Files.writeString(output, serialized, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        System.out.println("LIVE_ASSISTANT_EVIDENCE=" + output);
        return output;
    }

    record HttpObservation(String label, int status, JsonNode response) { }
    record RecordedCall(String startedAtUtc, String completedAtUtc, List<DeepSeekMessage> messages, DeepSeekChatResult result) { }

    static final class RecordingClient extends DeepSeekChatClient {
        final DeepSeekChatClient delegate;
        final List<RecordedCall> calls = new ArrayList<>();
        int attempts;

        RecordingClient(DeepSeekChatClient delegate, ObjectMapper mapper) {
            super(new DeepSeekProperties(false, "", "synthetic-recorder", Duration.ofSeconds(3), Duration.ofSeconds(3), 1), mapper);
            this.delegate = delegate;
        }

        @Override
        public synchronized DeepSeekChatResult generate(List<DeepSeekMessage> messages) {
            assertTrue(attempts < 3, "Hard limit: this harness permits exactly three authorized adapter invocations");
            assertTrue(messages.size() == 3 || messages.size() == 5);
            assertEquals("system", messages.getFirst().role());
            assertTrue(messages.get(1).content().contains("SYNTHETIC_DAY36 fictional CV"));
            assertTrue(messages.get(1).content().contains("只读匹配分：75.00"));
            assertTrue(Set.of(FIRST, FOLLOWUP, HR).contains(messages.getLast().content()), "Only fixed synthetic questions may be sent");
            attempts++;
            String start = Instant.now().toString();
            DeepSeekChatResult result = delegate.generate(messages);
            calls.add(new RecordedCall(start, Instant.now().toString(), List.copyOf(messages), result));
            return result;
        }
    }

    // The profile excludes this test-only configuration from unrelated production-app test scans.
    @Configuration(proxyBeanMethods = false)
    @Profile("assistant-http-harness")
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class, RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class})
    @Import({SecurityConfiguration.class, JwtService.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class, RecruitmentChatController.class, RecruitmentChatService.class,
            GlobalExceptionHandler.class, DeepSeekConfiguration.class})
    static class Config {
        @Bean PlatformUserMapper users() { return mock(PlatformUserMapper.class); }
        @Bean RoleMapper roles() { return mock(RoleMapper.class); }
        @Bean JobApplicationMapper applications() { return mock(JobApplicationMapper.class); }
        @Bean JobPostingMapper jobs() { return mock(JobPostingMapper.class); }
        @Bean ResumeMapper resumes() { return mock(ResumeMapper.class); }
        @Bean AiMatchResultMapper matches() { return mock(AiMatchResultMapper.class); }
        @Bean @Primary RecordingClient recorder(@Qualifier("deepSeekChatClient") DeepSeekChatClient delegate, ObjectMapper mapper) {
            return new RecordingClient(delegate, mapper);
        }
    }
}
