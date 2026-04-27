package com.example.emotion_storage.user.auth.oauth.apple;

public record AppleLoginClaims (
        String subject,
        String email
){}
