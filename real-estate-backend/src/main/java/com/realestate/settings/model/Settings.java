package com.realestate.settings.model;

import com.realestate.messaging.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "broker_settings")
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "email_notifications_enabled", nullable = false)
    private boolean emailNotificationsEnabled = true;

    @Column(name = "lead_notifications_enabled", nullable = false)
    private boolean leadNotificationsEnabled = true;

    @Column(name = "site_visit_notifications_enabled", nullable = false)
    private boolean siteVisitNotificationsEnabled = true;

    @Column(name = "deal_notifications_enabled", nullable = false)
    private boolean dealNotificationsEnabled = true;

    @Column(name = "follow_up_notifications_enabled", nullable = false)
    private boolean followUpNotificationsEnabled = true;

    @Column(name = "document_notifications_enabled", nullable = false)
    private boolean documentNotificationsEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_dashboard_section", nullable = false, length = 30)
    private DashboardSection defaultDashboardSection = DashboardSection.OVERVIEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ThemePreference theme = ThemePreference.SYSTEM;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    public void setEmailNotificationsEnabled(boolean value) {
        emailNotificationsEnabled = value;
    }

    public boolean isLeadNotificationsEnabled() {
        return leadNotificationsEnabled;
    }

    public void setLeadNotificationsEnabled(boolean value) {
        leadNotificationsEnabled = value;
    }

    public boolean isSiteVisitNotificationsEnabled() {
        return siteVisitNotificationsEnabled;
    }

    public void setSiteVisitNotificationsEnabled(boolean value) {
        siteVisitNotificationsEnabled = value;
    }

    public boolean isDealNotificationsEnabled() {
        return dealNotificationsEnabled;
    }

    public void setDealNotificationsEnabled(boolean value) {
        dealNotificationsEnabled = value;
    }

    public boolean isFollowUpNotificationsEnabled() {
        return followUpNotificationsEnabled;
    }

    public void setFollowUpNotificationsEnabled(boolean value) {
        followUpNotificationsEnabled = value;
    }

    public boolean isDocumentNotificationsEnabled() {
        return documentNotificationsEnabled;
    }

    public void setDocumentNotificationsEnabled(boolean value) {
        documentNotificationsEnabled = value;
    }

    public DashboardSection getDefaultDashboardSection() {
        return defaultDashboardSection;
    }

    public void setDefaultDashboardSection(DashboardSection defaultDashboardSection) {
        this.defaultDashboardSection = defaultDashboardSection;
    }

    public ThemePreference getTheme() {
        return theme;
    }

    public void setTheme(ThemePreference theme) {
        this.theme = theme;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
