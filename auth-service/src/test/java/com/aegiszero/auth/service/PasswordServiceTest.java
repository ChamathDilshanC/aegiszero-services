package com.aegiszero.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordService(Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());

    @Test
    void hashPassword_verifiesWithMatches() {
        String hash = passwordService.hashPassword("SuperSecret123!");

        assertThat(passwordService.matches("SuperSecret123!", hash)).isTrue();
        assertThat(passwordService.matches("WrongPassword!", hash)).isFalse();
    }

    @Test
    void hashPassword_isSaltedSoRepeatedHashesDiffer() {
        String hash1 = passwordService.hashPassword("SuperSecret123!");
        String hash2 = passwordService.hashPassword("SuperSecret123!");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void hashToken_isDeterministicForLookupByHash() {
        String token = passwordService.generateOneTimeToken();

        assertThat(passwordService.hashToken(token)).isEqualTo(passwordService.hashToken(token));
    }

    @Test
    void hashToken_differsForDifferentTokens() {
        String a = passwordService.hashToken("token-a");
        String b = passwordService.hashToken("token-b");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void generateOneTimeToken_producesHighEntropyDistinctValues() {
        String a = passwordService.generateOneTimeToken();
        String b = passwordService.generateOneTimeToken();

        assertThat(a).isNotEqualTo(b);
        assertThat(a.length()).isGreaterThanOrEqualTo(32);
    }
}
