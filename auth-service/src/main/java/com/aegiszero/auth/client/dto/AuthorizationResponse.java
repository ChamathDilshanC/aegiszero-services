package com.aegiszero.auth.client.dto;

import java.util.List;

public record AuthorizationResponse(
        List<String> roles,
        List<String> permissions
) {
    public static AuthorizationResponse empty() {
        return new AuthorizationResponse(List.of(), List.of());
    }
}
