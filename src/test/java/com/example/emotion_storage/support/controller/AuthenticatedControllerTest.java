package com.example.emotion_storage.support.controller;

import com.example.emotion_storage.support.security.WithMockCustomUser;

@WithMockCustomUser(id = 1L, email = "test@example.com")
public abstract class AuthenticatedControllerTest {
}
