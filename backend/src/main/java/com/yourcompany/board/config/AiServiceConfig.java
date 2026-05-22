package com.yourcompany.board.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.service")
public class AiServiceConfig {
    private String analyzeUrl;
    private String conversationUrl;

    public String getAnalyzeUrl() {
        return analyzeUrl;
    }
    public void setAnalyzeUrl(String analyzeUrl) {
        this.analyzeUrl = analyzeUrl;
    }
    public String getConversationUrl() {
        return conversationUrl;
    }
    public void setConversationUrl(String conversationUrl) {
        this.conversationUrl = conversationUrl;
    }
}