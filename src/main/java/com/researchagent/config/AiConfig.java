package com.researchagent.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Bean
    public OllamaChatModel ollamaChatModel(
            @Value("${ai.ollama.base-url:http://127.0.0.1:11434}") String baseUrl,
            @Value("${ai.ollama.model:phi3:mini}") String modelName) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.2)
                .timeout(Duration.ofSeconds(180))
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${ai.ollama.base-url:http://127.0.0.1:11434}") String baseUrl,
            @Value("${ai.ollama.embedding-model:nomic-embed-text}") String modelName) {
        return OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(180))
                .build();
    }
}
