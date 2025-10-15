package com.example.emotion_storage.user.auth.oauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OauthConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    // Kakao 전용 RestClient
    @Bean
    public RestClient kakaoRestClient(RestClient.Builder builder) {
        return builder.build();
    }

    // Apple 전용 RestClient
    @Bean
    public RestClient appleRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}
