package com.aegiszero.auth.client.dto;

public record MfaChallengeRequest(
        String userId,
        String method,
        String email
) {
}
