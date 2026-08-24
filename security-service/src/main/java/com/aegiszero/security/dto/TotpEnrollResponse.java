package com.aegiszero.security.dto;

public record TotpEnrollResponse(
        String secret,
        String otpauthUrl
) {
}
