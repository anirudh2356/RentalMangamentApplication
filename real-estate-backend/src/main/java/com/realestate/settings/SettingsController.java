package com.realestate.settings;

import com.realestate.settings.dto.SettingsResponse;
import com.realestate.settings.dto.SettingsUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public SettingsResponse get(Authentication authentication) {
        return settingsService.get(authentication.getName());
    }

    @PutMapping
    public SettingsResponse update(
            Authentication authentication,
            @Valid @RequestBody SettingsUpdateRequest request
    ) {
        return settingsService.update(authentication.getName(), request);
    }
}
