package com.example.emotion_storage.user.auth.oauth.apple;

import java.util.List;

public record AppleJwksResponse(
        List<AppleJwk> keys
) {
    public record AppleJwk(
            String kty,
            String kid,
            String use,
            String alg,
            String n,
            String e
    ) {}
}
