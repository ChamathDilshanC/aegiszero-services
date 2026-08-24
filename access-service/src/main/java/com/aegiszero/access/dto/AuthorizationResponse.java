package com.aegiszero.access.dto;

import java.util.List;

public record AuthorizationResponse(
        List<String> roles,
        List<String> permissions
) {
}
