package com.example.emotion_storage.user.auth.oauth.apple;

import com.example.emotion_storage.global.exception.BaseException;
import com.example.emotion_storage.global.exception.ErrorCode;
import com.example.emotion_storage.user.auth.oauth.apple.AppleJwksResponse.AppleJwk;
import com.example.emotion_storage.user.auth.oauth.apple.config.AppleOauthProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppleTokenVerifier {

    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final AppleJwksClient jwksClient;
    private final AppleOauthProperties appleOauthProperties;

    public AppleLoginClaims verifyLoginToken(String idToken) {
        SignedJWT signedJWT = parse(idToken);

        String kid = signedJWT.getHeader().getKeyID();
        if (kid == null || kid.isBlank()) {
            throw new BaseException(ErrorCode.INVALID_APPLE_ID_TOKEN);
        }

        AppleJwk jwk = jwksClient.fetchKeys().keys().stream()
                .filter(k -> kid.equals(k.kid()))
                .findFirst()
                .orElseThrow(() -> new BaseException(ErrorCode.APPLE_PUBLIC_KEY_NOT_FOUND));

        RSAPublicKey publicKey = toPublicKey(jwk);

        verifySignature(signedJWT, publicKey);

        JWTClaimsSet claims = getClaims(signedJWT);
        validateClaims(claims);

        return new AppleLoginClaims(
                claims.getSubject(),
                safeStringClaim(claims, "email")
        );
    }

    private SignedJWT parse(String idToken) {
        try {
            return SignedJWT.parse(idToken);
        } catch (ParseException e) {
            throw new BaseException(ErrorCode.INVALID_APPLE_ID_TOKEN);
        }
    }

    private void verifySignature(SignedJWT jwt, RSAPublicKey publicKey) {
        try {
            boolean ok = jwt.verify(new RSASSAVerifier(publicKey));
            if (!ok) {
                throw new BaseException(ErrorCode.INVALID_APPLE_ID_TOKEN_SIGNATURE);
            }
        } catch (JOSEException e) {
            throw new BaseException(ErrorCode.INVALID_APPLE_ID_TOKEN_SIGNATURE);
        }
    }

    private JWTClaimsSet getClaims(SignedJWT jwt) {
        try {
            return jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new BaseException(ErrorCode.INVALID_APPLE_ID_TOKEN);
        }
    }

    private void validateClaims(JWTClaimsSet claims) {
        // iss
        if (!APPLE_ISSUER.equals(claims.getIssuer())) {
            throw new BaseException(ErrorCode.INVALID_APPLE_ID_TOKEN_CLAIMS);
        }

        // aud(Bundle ID)
        String audience = appleOauthProperties.audience();
        if (claims.getAudience() == null || claims.getAudience().stream().noneMatch(audience::equals)) {
            throw new BaseException(ErrorCode.INVALID_APPLE_ID_TOKEN_CLAIMS);
        }

        // exp
        Date exp = claims.getExpirationTime();
        if (exp == null || exp.toInstant().isBefore(Instant.now())) {
            throw new BaseException(ErrorCode.APPLE_ID_TOKEN_EXPIRED);
        }

        // sub
        String sub = claims.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new BaseException(ErrorCode.INVALID_APPLE_ID_TOKEN_CLAIMS);
        }
    }

    private RSAPublicKey toPublicKey(AppleJwk jwk) {
        try {
            BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.n()));
            BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.e()));
            RSAPublicKeySpec spec = new RSAPublicKeySpec(n, e);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new BaseException(ErrorCode.APPLE_PUBLIC_KEY_BUILD_FAILED);
        }
    }

    private String safeStringClaim(JWTClaimsSet claims, String key) {
        try {
            return claims.getStringClaim(key);
        } catch (Exception e) {
            return null;
        }
    }
}
