package com.example.emotion_storage.global.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/session").authenticated()
                        .requestMatchers("/auth/**",
                                "/ws/**",
                                "/api/v1/users/login/**",
                                "/api/v1/users/signup/**",
                                "/docs/**",
                                "/health",
                                "/h2-console/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated());
//                        .requestMatchers("/auth/session").authenticated()
//                        .anyRequest().permitAll()
        return httpSecurity.build();
    }
}
