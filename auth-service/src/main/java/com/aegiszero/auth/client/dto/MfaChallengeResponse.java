package com.aegiszero.auth.client.dto;

public record MfaChallengeResponse(
        String challengeId,
        String method,
        String userId
) {
}
