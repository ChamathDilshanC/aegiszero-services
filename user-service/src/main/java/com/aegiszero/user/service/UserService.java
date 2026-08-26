package com.aegiszero.user.service;

import com.aegiszero.common.exception.ResourceNotFoundException;
import com.aegiszero.user.dto.PageResponse;
import com.aegiszero.user.dto.UpdateProfileRequest;
import com.aegiszero.user.dto.UserResponse;
import com.aegiszero.user.entity.User;
import com.aegiszero.user.entity.UserStatus;
import com.aegiszero.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void createFromRegistration(UUID userId, String email, String firstName, String lastName) {
        if (userRepository.existsById(userId)) {
            return;
        }
        User user = User.builder()
                .id(userId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .build();
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return UserResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String query, Pageable pageable) {
        String q = (query == null || query.isBlank()) ? "" : query.trim();
        return PageResponse.from(userRepository.search(q, pageable).map(UserResponse::from));
    }

    @Transactional
    public UserResponse update(UUID id, UpdateProfileRequest request) {
        User user = findOrThrow(id);
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void disable(UUID id) {
        User user = findOrThrow(id);
        user.setStatus(UserStatus.DISABLED);
        userRepository.save(user);
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
