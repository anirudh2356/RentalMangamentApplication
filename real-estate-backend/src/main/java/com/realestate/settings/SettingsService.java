package com.realestate.settings;

import com.realestate.messaging.model.Role;
import com.realestate.messaging.model.User;
import com.realestate.messaging.repository.UserRepository;
import com.realestate.settings.dto.SettingsResponse;
import com.realestate.settings.dto.SettingsUpdateRequest;
import com.realestate.settings.model.Settings;
import com.realestate.settings.repository.SettingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SettingsService {

    private final UserRepository userRepository;
    private final SettingsRepository settingsRepository;

    public SettingsService(UserRepository userRepository, SettingsRepository settingsRepository) {
        this.userRepository = userRepository;
        this.settingsRepository = settingsRepository;
    }

    @Transactional
    public SettingsResponse get(String principalEmail) {
        User broker = requireBroker(principalEmail);
        Settings settings = settingsRepository.findByUserId(broker.getId())
                .orElseGet(() -> {
                    Settings created = new Settings();
                    created.setUser(broker);
                    return settingsRepository.save(created);
                });
        return toResponse(settings);
    }

    @Transactional
    public SettingsResponse update(String principalEmail, SettingsUpdateRequest request) {
        User broker = requireBroker(principalEmail);
        Settings settings = settingsRepository.findByUserId(broker.getId())
                .orElseGet(() -> {
                    Settings created = new Settings();
                    created.setUser(broker);
                    return created;
                });
        settings.setEmailNotificationsEnabled(request.emailNotificationsEnabled());
        settings.setLeadNotificationsEnabled(request.leadNotificationsEnabled());
        settings.setSiteVisitNotificationsEnabled(request.siteVisitNotificationsEnabled());
        settings.setDealNotificationsEnabled(request.dealNotificationsEnabled());
        settings.setFollowUpNotificationsEnabled(request.followUpNotificationsEnabled());
        settings.setDocumentNotificationsEnabled(request.documentNotificationsEnabled());
        settings.setDefaultDashboardSection(request.defaultDashboardSection());
        settings.setTheme(request.theme());
        return toResponse(settingsRepository.save(settings));
    }

    private User requireBroker(String principalEmail) {
        User user = userRepository.findByEmailIgnoreCase(principalEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Broker account not found"));
        if (user.getRole() != Role.BROKER || !user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker access required");
        }
        return user;
    }

    private SettingsResponse toResponse(Settings settings) {
        return new SettingsResponse(
                settings.isEmailNotificationsEnabled(),
                settings.isLeadNotificationsEnabled(),
                settings.isSiteVisitNotificationsEnabled(),
                settings.isDealNotificationsEnabled(),
                settings.isFollowUpNotificationsEnabled(),
                settings.isDocumentNotificationsEnabled(),
                settings.getDefaultDashboardSection(),
                settings.getTheme(),
                settings.getCreatedAt(),
                settings.getUpdatedAt()
        );
    }
}
