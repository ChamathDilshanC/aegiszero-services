package com.aegiszero.auth.dto;

import lombok.Builder;

@Builder
public record LoginResponse(
        String status,
        TokenResponse tokens,
        String mfaChallengeId,
        String mfaMethod,
        String message
) {
    public static LoginResponse success(TokenResponse tokens) {
        return LoginResponse.builder().status("SUCCESS").tokens(tokens).build();
    }

    public static LoginResponse mfaRequired(String challengeId, String method) {
        return LoginResponse.builder()
                .status("MFA_REQUIRED")
                .mfaChallengeId(challengeId)
                .mfaMethod(method)
                .message("Multi-factor verification required")
                .build();
    }
}
