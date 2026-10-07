import { useEffect, useState } from 'react';
import { fetchBrokerSettings, updateBrokerSettings } from '../../api';
import './Settings.css';

const DEFAULT_SETTINGS = {
  emailNotificationsEnabled: true,
  leadNotificationsEnabled: true,
  siteVisitNotificationsEnabled: true,
  dealNotificationsEnabled: true,
  followUpNotificationsEnabled: true,
  documentNotificationsEnabled: true,
  defaultDashboardSection: 'OVERVIEW',
  theme: 'SYSTEM',
};

const notificationOptions = [
  ['emailNotificationsEnabled', 'Email Notifications', 'Receive account notifications by email.'],
  ['leadNotificationsEnabled', 'Lead Notifications', 'Updates when leads are created or changed.'],
  ['siteVisitNotificationsEnabled', 'Site Visit Notifications', 'Updates about scheduled site visits.'],
  ['dealNotificationsEnabled', 'Deal Notifications', 'Updates about your active and closed deals.'],
  ['followUpNotificationsEnabled', 'Follow-up Notifications', 'Reminders and updates for follow-up tasks.'],
  ['documentNotificationsEnabled', 'Document Notifications', 'Updates when broker documents change.'],
];

const dashboardSections = [
  ['OVERVIEW', 'Overview'],
  ['LEADS', 'Leads'],
  ['PROPERTIES', 'Properties'],
  ['SITE_VISITS', 'Site Visits'],
  ['DEALS', 'Deals'],
  ['CALENDAR', 'Calendar'],
  ['ANALYTICS', 'Analytics'],
];

const themes = [
  ['LIGHT', 'Light'],
  ['DARK', 'Dark'],
  ['SYSTEM', 'System'],
];

function normalizeSettings(settings) {
  const result = { ...DEFAULT_SETTINGS };
  for (const key of Object.keys(DEFAULT_SETTINGS)) {
    if (Object.hasOwn(settings || {}, key)) result[key] = settings[key];
  }
  if (!dashboardSections.some(([value]) => value === result.defaultDashboardSection)) {
    result.defaultDashboardSection = 'OVERVIEW';
  }
  if (!themes.some(([value]) => value === result.theme)) {
    result.theme = 'SYSTEM';
  }
  return result;
}

export default function Settings({ csrfToken, initialSettings, onSettingsUpdated }) {
  const [form, setForm] = useState(() => (
    initialSettings ? normalizeSettings(initialSettings) : { ...DEFAULT_SETTINGS }
  ));
  const [loading, setLoading] = useState(!initialSettings);
  const [settingsLoaded, setSettingsLoaded] = useState(Boolean(initialSettings));
  const [loadAttempt, setLoadAttempt] = useState(0);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    if (initialSettings) return undefined;
    let cancelled = false;
    fetchBrokerSettings()
      .then((settings) => {
        if (!cancelled) {
          setForm(normalizeSettings(settings));
          setSettingsLoaded(true);
        }
      })
      .catch((requestError) => {
        if (!cancelled) setError(requestError.message || 'Unable to load your settings.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [initialSettings, loadAttempt]);

  function setPreference(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
    setSuccess('');
    setError('');
  }

  async function saveSettings(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const saved = normalizeSettings(await updateBrokerSettings(form, csrfToken));
      setForm(saved);
      onSettingsUpdated?.(saved);
      setSuccess('Your settings have been saved.');
    } catch (requestError) {
      setError(requestError.message || 'Unable to save your settings.');
    } finally {
      setSaving(false);
    }
  }

  function resetSettings() {
    setForm({ ...DEFAULT_SETTINGS });
    setError('');
    setSuccess('');
  }

  return (
    <main className="broker-settings">
      <header className="settings-heading">
        <p className="settings-eyebrow">ACCOUNT</p>
        <h2>Settings</h2>
        <p>Choose how your broker workspace works for you.</p>
      </header>

      {error && settingsLoaded && <p className="settings-message settings-error" role="alert">{error}</p>}
      {success && <p className="settings-message settings-success" role="status">{success}</p>}
      {loading ? (
        <p className="settings-loading" role="status">Loading settings...</p>
      ) : !settingsLoaded ? (
        <div className="settings-load-error">
          <p className="settings-message settings-error" role="alert">
            {error || 'Settings could not be loaded. Your saved preferences have not been changed.'}
          </p>
          <button
            className="settings-retry"
            type="button"
            onClick={() => {
              setError('');
              setLoading(true);
              setLoadAttempt((attempt) => attempt + 1);
            }}
          >
            Try again
          </button>
        </div>
      ) : (
        <form onSubmit={saveSettings}>
          <section className="settings-section" aria-labelledby="notification-preferences-title">
            <div className="settings-section-heading">
              <h3 id="notification-preferences-title">Notification Preferences</h3>
              <p>Choose which updates you want to receive.</p>
            </div>
            <div className="settings-preference-list">
              {notificationOptions.map(([key, label, description]) => (
                <label className="settings-toggle-row" key={key}>
                  <span className="settings-toggle-copy">
                    <strong>{label}</strong>
                    <small>{description}</small>
                  </span>
                  <input
                    aria-label={label}
                    checked={form[key]}
                    className="settings-toggle"
                    type="checkbox"
                    onChange={(event) => setPreference(key, event.target.checked)}
                  />
                </label>
              ))}
            </div>
          </section>

          <section className="settings-section settings-select-section" aria-labelledby="dashboard-preferences-title">
            <div className="settings-section-heading">
              <h3 id="dashboard-preferences-title">Dashboard Preferences</h3>
              <p>Select the section to open when you enter your broker workspace.</p>
            </div>
            <label className="settings-select-label" htmlFor="default-dashboard-section">
              Default Dashboard Section
              <select
                id="default-dashboard-section"
                value={form.defaultDashboardSection}
                onChange={(event) => setPreference('defaultDashboardSection', event.target.value)}
              >
                {dashboardSections.map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
            </label>
          </section>

          <section className="settings-section" aria-labelledby="appearance-title">
            <div className="settings-section-heading">
              <h3 id="appearance-title">Appearance</h3>
              <p>Choose a theme for the broker dashboard and settings pages.</p>
            </div>
            <div className="settings-theme-options" role="radiogroup" aria-label="Theme">
              {themes.map(([value, label]) => (
                <label className={`settings-theme-option${form.theme === value ? ' is-selected' : ''}`} key={value}>
                  <input
                    checked={form.theme === value}
                    name="theme"
                    type="radio"
                    value={value}
                    onChange={(event) => setPreference('theme', event.target.value)}
                  />
                  <span className="settings-theme-indicator" aria-hidden="true" />
                  <span>
                    <strong>{label}</strong>
                    <small>
                      {value === 'SYSTEM'
                        ? 'Follow your device preference'
                        : `Use the ${label.toLowerCase()} dashboard theme`}
                    </small>
                  </span>
                </label>
              ))}
            </div>
          </section>

          <div className="settings-actions">
            <button className="settings-reset" type="button" onClick={resetSettings} disabled={saving}>
              Reset
            </button>
            <button className="settings-save" type="submit" disabled={saving}>
              {saving ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      )}
    </main>
  );
}
