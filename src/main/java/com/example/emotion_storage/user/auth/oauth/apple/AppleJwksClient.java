package com.example.emotion_storage.user.auth.oauth.apple;

import com.example.emotion_storage.global.exception.BaseException;
import com.example.emotion_storage.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class AppleJwksClient {

    public static final String APPLE_JWKS_URI = "https://appleid.apple.com/auth/keys";
    
    private final RestClient appleRestClient;

    public AppleJwksResponse fetchKeys() {
        try {
            AppleJwksResponse response = appleRestClient.get()
                    .uri(APPLE_JWKS_URI)
                    .retrieve()
                    .body(AppleJwksResponse.class);

            if (response == null || response.keys() == null || response.keys().isEmpty()) {
                throw new BaseException(ErrorCode.APPLE_JWKS_FETCH_FAILED);
            }
            return response;
        } catch (RestClientResponseException e) {
            throw new BaseException(ErrorCode.APPLE_JWKS_FETCH_FAILED);
        }
    }
}
