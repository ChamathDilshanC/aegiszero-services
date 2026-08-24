package com.aegiszero.user.dto;

import com.aegiszero.user.entity.User;

public record UserResponse(
        String id,
        String email,
        String firstName,
        String lastName,
        String avatarUrl,
        String status,
        String createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getAvatarUrl(),
                user.getStatus().name(),
                user.getCreatedAt().toString()
        );
    }
}
