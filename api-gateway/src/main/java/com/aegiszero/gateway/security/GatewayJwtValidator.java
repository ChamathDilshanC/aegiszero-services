package com.aegiszero.gateway.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Edge-level JWT signature/expiry check only. Every downstream service still
 * independently validates the token and its own permission requirements -
 * this is a first line of defense, not the authority.
 */
@Component
@EnableConfigurationProperties(GatewayJwtProperties.class)
public class GatewayJwtValidator {

    private final SecretKey signingKey;
    private final String issuer;

    public GatewayJwtValidator(GatewayJwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.issuer = properties.getIssuer();
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
