package com.rag.chatbot.config;

import lombok.RequiredArgsConstructor;
import lombok.Getter;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Getter
public class DeepSeekConfig {

    private final DeepSeekProperties deepSeekProperties;
}

