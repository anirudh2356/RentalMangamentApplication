package com.realestate.settings.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.realestate.settings.model.DashboardSection;
import com.realestate.settings.model.ThemePreference;
import jakarta.validation.constraints.NotNull;

public record SettingsUpdateRequest(
        @NotNull Boolean emailNotificationsEnabled,
        @NotNull Boolean leadNotificationsEnabled,
        @NotNull Boolean siteVisitNotificationsEnabled,
        @NotNull Boolean dealNotificationsEnabled,
        @NotNull Boolean followUpNotificationsEnabled,
        @NotNull Boolean documentNotificationsEnabled,
        @NotNull DashboardSection defaultDashboardSection,
        @NotNull ThemePreference theme
) {
    @JsonAnySetter
    public void rejectUnsupportedField(String field, Object value) {
        throw new IllegalArgumentException("Unsupported settings field: " + field);
    }
}
