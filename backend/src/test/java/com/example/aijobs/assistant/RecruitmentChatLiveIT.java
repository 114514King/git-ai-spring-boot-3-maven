package com.example.aijobs.assistant;

import com.example.aijobs.deepseek.DeepSeekChatResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/** Explicit opt-in; three official API calls, fixed synthetic mapper data, never a database E2E claim. */
@EnabledIfEnvironmentVariable(named = "ASSISTANT_LIVE_TEST", matches = "true")
@SpringBootTest(classes = RecruitmentChatHttpHarness.Config.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.address=127.0.0.1", "app.jwt.secret=day36-synthetic-http-test-secret-at-least-32-bytes",
                "app.jwt.expiration=PT2H", "app.deepseek.enabled=true", "app.deepseek.api-key=${DEEPSEEK_API_KEY:}",
                "app.deepseek.max-tokens=2048", "app.deepseek.request-timeout=PT30S", "app.external-jobs.enabled=false"})
class RecruitmentChatLiveIT extends RecruitmentChatHttpHarness {
    @Test
    void officialResponsesTraverseStudentHistoryAndHrHttpFlowWithoutBusinessWrites() throws Exception {
        assertFalse(properties.apiKey().isBlank(), "DEEPSEEK_API_KEY is required in the runtime environment");
        try {
            exerciseHttpFlow();
        } finally {
            // Keep safe diagnostics even if HTTP, authorization or state assertions fail.
            saveLiveEvidence();
        }
        for (RecordedCall call : recorder.calls) {
            DeepSeekChatResult result = call.result();
            assertAll("Official adapter response",
                    () -> assertEquals(DeepSeekChatResult.Status.GENERATED, result.status()),
                    () -> assertEquals(Integer.valueOf(200), result.httpStatus()),
                    () -> assertEquals("stop", result.finishReason(), "Incomplete advice must not count as a complete live result"),
                    () -> assertFalse(result.truncated()),
                    () -> assertFalse(result.content().isBlank()),
                    () -> assertNotNull(result.usage()));
            assertTrue(result.usage().promptTokens() > 0);
            assertTrue(result.usage().completionTokens() > 0);
            assertEquals((long) result.usage().promptTokens() + result.usage().completionTokens(), (long) result.usage().totalTokens());
        }
        for (HttpObservation observation : http) {
            if (observation.status() == 200) {
                assertTrue(observation.response().path("data").path("generated").asBoolean());
                assertEquals("GENERATED", observation.response().path("data").path("modelStatus").asText());
            }
        }
        System.out.println("LIVE_ASSISTANT_STATUS generated=3/3 humanQuality=UNVERIFIED persistence=MOCKED");
    }
}
