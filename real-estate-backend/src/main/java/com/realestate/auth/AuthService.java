package com.realestate.auth;

import com.realestate.auth.dto.AuthenticatedUserResponse;
import com.realestate.auth.dto.LoginRequest;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    public Authentication authenticate(LoginRequest request) {
        String email = request.email().trim();
        return authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        email,
                        request.password()
                )
        );
    }

    @Transactional(readOnly = true)
    public AuthenticatedUserResponse getUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated account no longer exists"));

        return new AuthenticatedUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
