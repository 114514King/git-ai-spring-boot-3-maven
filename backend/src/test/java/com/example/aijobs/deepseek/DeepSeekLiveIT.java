package com.example.aijobs.deepseek;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "DEEPSEEK_LIVE_TEST", matches = "true")
class DeepSeekLiveIT {
    @Test
    void verifiesOfficialApiUsingOnlySyntheticTeachingMessages() {
        var key = System.getenv("DEEPSEEK_API_KEY");
        assertTrue(key != null && !key.isBlank(), "DEEPSEEK_API_KEY is required; never put it in source files");
        var model = System.getenv().getOrDefault("DEEPSEEK_MODEL", "deepseek-flash");
        var properties = new DeepSeekProperties(true, key, model, Duration.ofSeconds(3), Duration.ofSeconds(15), 128);
        try (var client = new DeepSeekChatClient(properties, new ObjectMapper())) {
            var result = client.generate(List.of(
                    new DeepSeekMessage("system", "Use only the synthetic facts. A human makes all hiring decisions."),
                    new DeepSeekMessage("user", "Synthetic candidate mentions English and lesson planning. "
                            + "Synthetic job also requires classroom management. Give one evidence-check question. "
                            + "Do not score or make a hiring decision.")));
            assertTrue(result.generated(), "Real API call did not generate text: " + result.status()
                    + ", HTTP status: " + result.httpStatus());
            assertFalse(result.content().isBlank());
            assertNotNull(result.usage(), "Verify that the real API reports token usage");
        }
    }
}
