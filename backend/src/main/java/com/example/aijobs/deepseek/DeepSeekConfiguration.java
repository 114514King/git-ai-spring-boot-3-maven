package com.example.aijobs.deepseek;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DeepSeekProperties.class)
public class DeepSeekConfiguration {
    @Bean(destroyMethod = "close")
    DeepSeekChatClient deepSeekChatClient(DeepSeekProperties properties, ObjectMapper mapper) {
        return new DeepSeekChatClient(properties, mapper);
    }
}
