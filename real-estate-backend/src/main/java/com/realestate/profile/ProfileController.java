package com.realestate.profile;

import com.realestate.profile.dto.PasswordChangeRequest;
import com.realestate.profile.dto.ProfileResponse;
import com.realestate.profile.dto.ProfileUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final SecurityContextRepository securityContextRepository;

    public ProfileController(ProfileService profileService, SecurityContextRepository securityContextRepository) {
        this.profileService = profileService;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping
    public ProfileResponse get(Authentication authentication) {
        return profileService.get(authentication.getName());
    }

    @PutMapping
    public ProfileResponse update(
            Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        ProfileResponse profile = profileService.update(authentication.getName(), request);
        if (!profile.email().equalsIgnoreCase(authentication.getName())) {
            UsernamePasswordAuthenticationToken updatedAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                    profile.email(),
                    authentication.getCredentials(),
                    authentication.getAuthorities()
            );
            updatedAuthentication.setDetails(authentication.getDetails());
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(updatedAuthentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, servletRequest, servletResponse);
        }
        return profile;
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            Authentication authentication,
            @Valid @RequestBody PasswordChangeRequest request
    ) {
        profileService.changePassword(authentication.getName(), request);
    }
}
