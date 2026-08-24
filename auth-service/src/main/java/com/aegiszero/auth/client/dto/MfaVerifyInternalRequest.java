package com.aegiszero.auth.client.dto;

public record MfaVerifyInternalRequest(
        String challengeId,
        String code
) {
}
