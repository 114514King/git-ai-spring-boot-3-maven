package com.example.aijobs.assistant;

import com.example.aijobs.deepseek.DeepSeekChatResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RecruitmentChatHttpHarness.Config.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.address=127.0.0.1", "app.jwt.secret=day36-synthetic-http-test-secret-at-least-32-bytes",
                "app.jwt.expiration=PT2H", "app.deepseek.enabled=false", "app.deepseek.api-key=", "app.external-jobs.enabled=false"})
class RecruitmentChatHttpTests extends RecruitmentChatHttpHarness {
    @Test
    void realHttpAuthenticationContextHistoryAndReadOnlyStateWithProductionDisabledAdapter() throws Exception {
        exerciseHttpFlow();
        for (RecordedCall call : recorder.calls) {
            assertEquals(DeepSeekChatResult.Status.DISABLED, call.result().status());
            assertFalse(call.result().generated());
            assertNull(call.result().httpStatus());
        }
        for (HttpObservation observation : http) {
            if (observation.status() == 200) {
                assertFalse(observation.response().path("data").path("generated").asBoolean());
                assertEquals("DISABLED", observation.response().path("data").path("modelStatus").asText());
            }
        }
    }
}
