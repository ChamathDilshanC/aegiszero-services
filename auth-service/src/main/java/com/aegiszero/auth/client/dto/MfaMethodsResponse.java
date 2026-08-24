package com.aegiszero.auth.client.dto;

import java.util.List;

public record MfaMethodsResponse(
        boolean enabled,
        List<String> methods
) {
}
