package com.aegiszero.security.dto;

import java.util.List;

public record MfaMethodsResponse(
        boolean enabled,
        List<String> methods
) {
}
