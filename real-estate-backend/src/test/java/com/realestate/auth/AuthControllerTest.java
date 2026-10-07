package com.realestate.auth;

import com.realestate.auth.dto.AuthenticatedUserResponse;
import com.realestate.auth.dto.LoginRequest;
import com.realestate.messaging.config.SecurityConfig;
import com.realestate.messaging.model.Role;
import com.realestate.messaging.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AuthExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void currentUserEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void currentUserEndpointReturnsAuthenticatedProfile() throws Exception {
        when(authService.getUser("alice@example.local"))
                .thenReturn(new AuthenticatedUserResponse(
                        1L,
                        "Alice",
                        "alice@example.local",
                        Role.BUYER
                ));

        mockMvc.perform(get("/api/auth/me")
                        .with(user("alice@example.local").roles("BUYER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.local"))
                .andExpect(jsonPath("$.role").value("BUYER"));
    }

    @Test
    void currentUserEndpointReturnsBobAsBrokerWhenSessionBelongsToBob() throws Exception {
        when(authService.getUser("bob@example.local"))
                .thenReturn(new AuthenticatedUserResponse(
                        2L,
                        "Bob",
                        "bob@example.local",
                        Role.BROKER
                ));

        mockMvc.perform(get("/api/auth/me")
                        .with(user("bob@example.local").roles("BROKER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Bob"))
                .andExpect(jsonPath("$.email").value("bob@example.local"))
                .andExpect(jsonPath("$.role").value("BROKER"));
    }

    @Test
    void successfulLoginCreatesSessionAndAllowsCurrentUserRequest() throws Exception {
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
                "alice@example.local",
                null,
                List.of(() -> "ROLE_BUYER")
        );
        AuthenticatedUserResponse profile = new AuthenticatedUserResponse(
                1L,
                "Alice",
                "alice@example.local",
                Role.BUYER
        );
        when(authService.authenticate(any(LoginRequest.class))).thenReturn(authenticated);
        when(authService.getUser("alice@example.local")).thenReturn(profile);

        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.local","password":"development-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.role").value("BUYER"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.local"));
    }

    @Test
    void invalidCredentialsReturnCleanUnauthorizedJson() throws Exception {
        when(authService.authenticate(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.local","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }
}
