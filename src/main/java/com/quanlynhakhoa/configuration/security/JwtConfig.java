
package com.quanlynhakhoa.configuration.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Configuration
public class JwtConfig {

    @Bean
    public SecretKey jwtSigningKey(
            @Value("${n08.security.jwt.secret-base64}")
            String secretBase64
    ) {
        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(secretBase64);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "JWT signing key must be valid Base64", ex
            );
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT signing key must contain at least 32 bytes"
            );
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return NimbusJwtEncoder
                .withSecretKey(jwtSigningKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSigningKey,
            @Value("${n08.security.jwt.issuer}") String issuer,
            @Value("${n08.security.jwt.audience}") String audience
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtClaimValidator<List<String>>(
                        JwtClaimNames.AUD,
                        audiences -> audiences != null
                                && audiences.contains(audience)
                );

        OAuth2TokenValidator<Jwt> tokenTypeValidator =
                new JwtClaimValidator<String>(
                        "token_type",
                        "access"::equals
                );

        OAuth2TokenValidator<Jwt> sessionIdValidator =
                new JwtClaimValidator<String>(
                        "sid",
                        sessionId -> {
                            if (sessionId == null) {
                                return false;
                            }

                            try {
                                UUID.fromString(sessionId);
                                return true;
                            } catch (IllegalArgumentException ex) {
                                return false;
                            }
                        }
                );

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(issuer),
                        audienceValidator,
                        tokenTypeValidator,
                        sessionIdValidator
                )
        );

        return decoder;
    }
}
