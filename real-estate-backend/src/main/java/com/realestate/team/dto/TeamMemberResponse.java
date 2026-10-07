package com.realestate.team.dto;

import com.realestate.messaging.model.Role;

import java.time.Instant;

public record TeamMemberResponse(
        Long id,
        String name,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt
) {
}
