package com.aegiszero.security.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TotpUtilTest {

    /**
     * Base32 of the ASCII string "12345678901234567890", the shared secret used
     * by every RFC 6238 (Appendix B) SHA-1 test vector.
     */
    private static final String RFC6238_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Test
    void generateCode_matchesRfc6238VectorAtCounter1() {
        // T = 59s / 30s step = counter 1. RFC vector (8-digit) is 94287082;
        // this implementation truncates to 6 digits, i.e. the low 6 digits: 287082.
        byte[] key = TotpUtil.base32Decode(RFC6238_SECRET);
        assertThat(TotpUtil.generateCode(key, 1)).isEqualTo("287082");
    }

    @Test
    void generateCode_matchesRfc6238VectorAtCounter37037036() {
        // T = 1111111109s / 30s step = counter 37037036. RFC vector is 07081804 -> 081804.
        byte[] key = TotpUtil.base32Decode(RFC6238_SECRET);
        assertThat(TotpUtil.generateCode(key, 37037036)).isEqualTo("081804");
    }

    @Test
    void verify_rejectsWrongCode() {
        assertThat(TotpUtil.verify(TotpUtil.generateBase32Secret(), "000000")).isFalse();
    }

    @Test
    void verify_rejectsNonNumericCode() {
        assertThat(TotpUtil.verify(TotpUtil.generateBase32Secret(), "abcdef")).isFalse();
    }

    @Test
    void verify_acceptsCodeGeneratedForCurrentStep() {
        String secret = TotpUtil.generateBase32Secret();
        long currentStep = System.currentTimeMillis() / 1000 / 30;
        String code = TotpUtil.generateCode(TotpUtil.base32Decode(secret), currentStep);

        assertThat(TotpUtil.verify(secret, code)).isTrue();
    }

    @Test
    void generateBase32Secret_producesDistinctSecrets() {
        assertThat(TotpUtil.generateBase32Secret()).isNotEqualTo(TotpUtil.generateBase32Secret());
    }
}
