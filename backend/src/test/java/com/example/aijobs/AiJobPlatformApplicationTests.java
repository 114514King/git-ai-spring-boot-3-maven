package com.example.aijobs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AiJobPlatformApplicationTests {

    @Test
    void applicationClassCanBeLoaded() {
        assertDoesNotThrow(() -> Class.forName("com.example.aijobs.AiJobPlatformApplication"));
    }
}
