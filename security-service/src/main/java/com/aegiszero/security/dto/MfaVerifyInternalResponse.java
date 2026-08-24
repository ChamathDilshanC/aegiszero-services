package com.aegiszero.security.dto;

public record MfaVerifyInternalResponse(
        boolean verified,
        String userId
) {
}
