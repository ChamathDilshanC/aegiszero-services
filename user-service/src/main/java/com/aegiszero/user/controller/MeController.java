package com.aegiszero.user.controller;

import com.aegiszero.common.exception.UnauthorizedException;
import com.aegiszero.common.security.jwt.AegisPrincipal;
import com.aegiszero.user.dto.UpdateProfileRequest;
import com.aegiszero.user.dto.UserResponse;
import com.aegiszero.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    @GetMapping
    public UserResponse me(Authentication authentication) {
        return userService.get(currentUserId(authentication));
    }

    @PutMapping
    public UserResponse updateMe(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.update(currentUserId(authentication), request);
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AegisPrincipal principal) {
            return UUID.fromString(principal.userId());
        }
        throw new UnauthorizedException("Authentication required");
    }
}
