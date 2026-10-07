package com.realestate.profile.dto;

import com.realestate.messaging.model.Role;

import java.time.Instant;

public record ProfileResponse(
        Long id,
        String name,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt
) {
}
