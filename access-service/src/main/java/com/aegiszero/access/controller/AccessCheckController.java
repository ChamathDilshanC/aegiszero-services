package com.aegiszero.access.controller;

import com.aegiszero.access.dto.AccessCheckRequest;
import com.aegiszero.access.dto.AccessCheckResponse;
import com.aegiszero.access.service.AuthorizationQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/access")
@RequiredArgsConstructor
public class AccessCheckController {

    private final AuthorizationQueryService authorizationQueryService;

    @PostMapping("/check")
    public AccessCheckResponse check(@Valid @RequestBody AccessCheckRequest request) {
        boolean allowed = authorizationQueryService.getAuthorization(UUID.fromString(request.userId()))
                .permissions()
                .contains(request.permission());
        return new AccessCheckResponse(allowed);
    }
}
