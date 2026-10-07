package com.realestate.auth.dto;

import com.realestate.messaging.model.Role;

public record AuthenticatedUserResponse(
        Long id,
        String name,
        String email,
        Role role
) {
}
