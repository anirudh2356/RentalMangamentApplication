package com.realestate.settings.dto;

import com.realestate.settings.model.DashboardSection;
import com.realestate.settings.model.ThemePreference;

import java.time.Instant;

public record SettingsResponse(
        boolean emailNotificationsEnabled,
        boolean leadNotificationsEnabled,
        boolean siteVisitNotificationsEnabled,
        boolean dealNotificationsEnabled,
        boolean followUpNotificationsEnabled,
        boolean documentNotificationsEnabled,
        DashboardSection defaultDashboardSection,
        ThemePreference theme,
        Instant createdAt,
        Instant updatedAt
) {
}
