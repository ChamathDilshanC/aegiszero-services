package com.aegiszero.security.dto;

public record MfaChallengeResponse(
        String challengeId,
        String method,
        String userId
) {
}
