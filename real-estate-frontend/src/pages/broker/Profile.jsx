import { useEffect, useState } from 'react';
import {
  changeProfilePassword,
  fetchProfile,
  updateProfile,
} from '../../api';
import './Profile.css';

function formatCreatedDate(value) {
  if (!value) return 'Not available';
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? 'Not available'
    : date.toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' });
}

function getInitials(name) {
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase();
}

export default function Profile({ csrfToken, authenticatedUser, onProfileUpdated }) {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({ name: '', email: '' });
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [changingPassword, setChangingPassword] = useState(false);
  const [passwordError, setPasswordError] = useState('');
  const [passwordSuccess, setPasswordSuccess] = useState('');
  const [showPasswords, setShowPasswords] = useState(false);

  useEffect(() => {
    let cancelled = false;
    fetchProfile()
      .then((data) => {
        if (!cancelled) {
          setProfile(data);
          setForm({ name: data.name || '', email: data.email || '' });
        }
      })
      .catch((requestError) => {
        if (!cancelled) setError(requestError.message || 'Unable to load your profile.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  function updateForm(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function updatePasswordForm(field, value) {
    setPasswordForm((current) => ({ ...current, [field]: value }));
  }

  function cancelEditing() {
    setForm({ name: profile.name || '', email: profile.email || '' });
    setEditing(false);
    setError('');
  }

  async function saveProfile(event) {
    event.preventDefault();
    setError('');
    setSaving(true);
    try {
      const updated = await updateProfile(
        { name: form.name.trim(), email: form.email.trim() },
        csrfToken,
      );
      setProfile(updated);
      setForm({ name: updated.name || '', email: updated.email || '' });
      setEditing(false);
      onProfileUpdated?.(updated);
    } catch (requestError) {
      setError(requestError.message || 'Unable to save your profile.');
    } finally {
      setSaving(false);
    }
  }

  async function savePassword(event) {
    event.preventDefault();
    setPasswordError('');
    setPasswordSuccess('');
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordError('The new password and confirmation do not match.');
      return;
    }
    if (passwordForm.newPassword.length < 8) {
      setPasswordError('Use at least 8 characters for your new password.');
      return;
    }

    setChangingPassword(true);
    try {
      await changeProfilePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      }, csrfToken);
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
      setPasswordSuccess('Your password has been changed.');
    } catch (requestError) {
      setPasswordError(requestError.message || 'Unable to change your password.');
    } finally {
      setChangingPassword(false);
    }
  }

  if (loading) {
    return <main className="broker-profile"><p className="profile-state">Loading profile...</p></main>;
  }

  if (!profile) {
    return (
      <main className="broker-profile">
        <h1>Broker profile</h1>
        <p className="profile-alert" role="alert">{error || 'Your profile could not be loaded.'}</p>
      </main>
    );
  }

  return (
    <main className="broker-profile">
      <header className="profile-heading">
        <div>
          <p className="profile-eyebrow">ACCOUNT</p>
          <h1>Broker profile</h1>
          <p>Manage your personal details and account password.</p>
        </div>
      </header>

      {error && <p className="profile-alert" role="alert">{error}</p>}

      <section className="profile-card" aria-labelledby="profile-details-title">
        <div className="profile-card-heading">
          <div className="profile-identity">
            <span className="profile-avatar" aria-hidden="true">
              {getInitials(profile.name || authenticatedUser?.name || '?')}
            </span>
            <div>
              <h2 id="profile-details-title">Personal details</h2>
              <p>Update the details associated with your broker account.</p>
            </div>
          </div>
          {!editing && (
            <button className="profile-button profile-button-secondary" type="button" onClick={() => setEditing(true)}>
              Edit profile
            </button>
          )}
        </div>

        {editing ? (
          <form className="profile-form" onSubmit={saveProfile}>
            <label>
              Full name
              <input
                autoComplete="name"
                maxLength={100}
                required
                value={form.name}
                onChange={(event) => updateForm('name', event.target.value)}
              />
            </label>
            <label>
              Email address
              <input
                autoComplete="email"
                maxLength={254}
                required
                type="email"
                value={form.email}
                onChange={(event) => updateForm('email', event.target.value)}
              />
            </label>
            <div className="profile-actions">
              <button className="profile-button profile-button-secondary" type="button" onClick={cancelEditing} disabled={saving}>
                Cancel
              </button>
              <button className="profile-button profile-button-primary" type="submit" disabled={saving}>
                {saving ? 'Saving...' : 'Save changes'}
              </button>
            </div>
          </form>
        ) : (
          <dl className="profile-details">
            <div><dt>Full name</dt><dd>{profile.name}</dd></div>
            <div><dt>Email address</dt><dd>{profile.email}</dd></div>
            <div><dt>Role</dt><dd><span className="profile-role">{profile.role}</span></dd></div>
            <div><dt>Account status</dt><dd><span className={`profile-status${profile.enabled ? ' is-enabled' : ''}`}>{profile.enabled ? 'Active' : 'Disabled'}</span></dd></div>
            <div><dt>Member since</dt><dd>{formatCreatedDate(profile.createdAt)}</dd></div>
          </dl>
        )}
      </section>

      <section className="profile-card" aria-labelledby="password-title">
        <div className="profile-card-heading">
          <div>
            <h2 id="password-title">Change password</h2>
            <p>Verify your current password before choosing a new one.</p>
          </div>
        </div>
        {passwordError && <p className="profile-alert" role="alert">{passwordError}</p>}
        {passwordSuccess && <p className="profile-success" role="status">{passwordSuccess}</p>}
        <form className="profile-form profile-password-form" onSubmit={savePassword}>
          <label>
            Current password
            <input
              autoComplete="current-password"
              required
              type={showPasswords ? 'text' : 'password'}
              value={passwordForm.currentPassword}
              onChange={(event) => updatePasswordForm('currentPassword', event.target.value)}
            />
          </label>
          <label>
            New password
            <input
              autoComplete="new-password"
              minLength={8}
              required
              type={showPasswords ? 'text' : 'password'}
              value={passwordForm.newPassword}
              onChange={(event) => updatePasswordForm('newPassword', event.target.value)}
            />
          </label>
          <label>
            Confirm new password
            <input
              autoComplete="new-password"
              minLength={8}
              required
              type={showPasswords ? 'text' : 'password'}
              value={passwordForm.confirmPassword}
              onChange={(event) => updatePasswordForm('confirmPassword', event.target.value)}
            />
          </label>
          <div className="profile-password-actions">
            <label className="profile-show-password">
              <input
                checked={showPasswords}
                type="checkbox"
                onChange={(event) => setShowPasswords(event.target.checked)}
              />
              Show passwords
            </label>
            <button className="profile-button profile-button-primary" type="submit" disabled={changingPassword}>
              {changingPassword ? 'Changing...' : 'Update password'}
            </button>
          </div>
        </form>
      </section>
    </main>
  );
}
