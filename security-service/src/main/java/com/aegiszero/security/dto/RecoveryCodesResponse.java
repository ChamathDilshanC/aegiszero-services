package com.aegiszero.security.dto;

import java.util.List;

public record RecoveryCodesResponse(
        List<String> codes
) {
}
