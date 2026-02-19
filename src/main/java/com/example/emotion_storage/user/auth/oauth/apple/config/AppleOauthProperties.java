package com.example.emotion_storage.user.auth.oauth.apple.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "apple")
public record AppleOauthProperties(
        String audience
) {}
