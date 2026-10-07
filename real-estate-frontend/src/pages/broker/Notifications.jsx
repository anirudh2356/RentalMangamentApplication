import { useEffect, useState } from 'react';
import {
  deleteNotification,
  fetchNotifications,
  fetchUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead,
} from '../../api';
import './Notifications.css';

function humanize(value) {
  return value
    ? value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (character) => character.toUpperCase())
    : 'Notification';
}

function formatTimestamp(value) {
  if (!value) return '—';
  return new Date(value).toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  });
}

function notifyCountChanged() {
  window.dispatchEvent(new Event('broker-notifications-updated'));
}

export default function Notifications({ csrfToken }) {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(null);
  const [filter, setFilter] = useState('all');
  const [loading, setLoading] = useState(true);
  const [actionId, setActionId] = useState(null);
  const [error, setError] = useState('');
  const [retryCount, setRetryCount] = useState(0);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      fetchNotifications(filter === 'unread'),
      fetchUnreadNotificationCount(),
    ])
      .then(([items, countResult]) => {
        if (!cancelled) {
          setNotifications(items);
          setUnreadCount(countResult.count);
          setError('');
        }
      })
      .catch((requestError) => {
        if (!cancelled) setError(requestError.message || 'Unable to load notifications.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [filter, retryCount]);

  async function refreshUnreadCount() {
    try {
      const result = await fetchUnreadNotificationCount();
      setUnreadCount(result.count);
    } catch {
      setUnreadCount(null);
    } finally {
      notifyCountChanged();
    }
  }

  async function handleMarkRead(notification) {
    if (notification.read) return;
    setActionId(notification.id);
    setError('');
    try {
      const updated = await markNotificationRead(notification.id, csrfToken);
      setNotifications((current) => current.map((item) => item.id === updated.id ? updated : item));
      await refreshUnreadCount();
    } catch (requestError) {
      setError(requestError.message || 'Unable to mark notification as read.');
    } finally {
      setActionId(null);
    }
  }

  async function handleMarkAllRead() {
    setActionId('all');
    setError('');
    try {
      await markAllNotificationsRead(csrfToken);
      if (filter === 'unread') {
        setNotifications([]);
      } else {
        setNotifications((current) => current.map((item) => ({ ...item, read: true })));
      }
      await refreshUnreadCount();
    } catch (requestError) {
      setError(requestError.message || 'Unable to mark notifications as read.');
    } finally {
      setActionId(null);
    }
  }

  async function handleDelete(notification) {
    if (!window.confirm('Delete this notification?')) return;
    setActionId(notification.id);
    setError('');
    try {
      await deleteNotification(notification.id, csrfToken);
      setNotifications((current) => current.filter((item) => item.id !== notification.id));
      await refreshUnreadCount();
    } catch (requestError) {
      setError(requestError.message || 'Unable to delete notification.');
    } finally {
      setActionId(null);
    }
  }

  return (
    <main className="broker-notifications-page">
      <header className="notifications-page-header">
        <div>
          <p className="notifications-kicker">WORKSPACE</p>
          <h2>Notifications</h2>
          <p>Updates from your leads, site visits, deals, follow-ups, and documents.</p>
        </div>
        <div className="notifications-unread-summary" aria-live="polite">
          <strong>{unreadCount === null ? '—' : unreadCount}</strong>
          <span>unread</span>
        </div>
      </header>

      <section className="notifications-toolbar" aria-label="Notification controls">
        <div className="notifications-tabs" role="tablist" aria-label="Notification filter">
          <button
            type="button"
            role="tab"
            aria-selected={filter === 'all'}
            className={filter === 'all' ? 'is-active' : ''}
            onClick={() => {
              setLoading(true);
              setFilter('all');
            }}
          >All</button>
          <button
            type="button"
            role="tab"
            aria-selected={filter === 'unread'}
            className={filter === 'unread' ? 'is-active' : ''}
            onClick={() => {
              setLoading(true);
              setFilter('unread');
            }}
          >Unread{unreadCount > 0 ? ` (${unreadCount})` : ''}</button>
        </div>
        <button
          className="notifications-mark-all"
          type="button"
          disabled={!unreadCount || actionId === 'all'}
          onClick={handleMarkAllRead}
        >{actionId === 'all' ? 'Marking…' : 'Mark all as read'}</button>
      </section>

      {error && (
        <section className="notifications-alert" role="alert">
          <span>{error}</span>
          <button type="button" onClick={() => {
            setLoading(true);
            setRetryCount((count) => count + 1);
          }}>Try again</button>
        </section>
      )}

      {loading ? (
        <section className="notifications-state" role="status">Loading notifications…</section>
      ) : error ? (
        <section className="notifications-state">Notifications could not be loaded.</section>
      ) : notifications.length ? (
        <section className="notifications-list" aria-label="Notifications">
          {notifications.map((notification) => (
            <article
              className={`notification-card${notification.read ? '' : ' is-unread'}`}
              key={notification.id}
            >
              <span className={`notification-type-icon notification-type-icon--${notification.type?.toLowerCase()}`} aria-hidden="true">
                {notification.type === 'LEAD' ? '◎'
                  : notification.type === 'SITE_VISIT' ? '⌂'
                    : notification.type === 'DEAL' ? '◇'
                      : notification.type === 'FOLLOW_UP' ? '↗'
                        : notification.type === 'DOCUMENT' ? '▤'
                          : notification.type === 'MESSAGE' ? '□' : '•'}
              </span>
              <div className="notification-copy">
                <div className="notification-title-row">
                  <h3>{notification.title}</h3>
                  {!notification.read && <span className="notification-unread-dot" aria-label="Unread" />}
                </div>
                <p>{notification.message}</p>
                <div className="notification-meta">
                  <span>{humanize(notification.type)}</span>
                  <time dateTime={notification.createdAt}>{formatTimestamp(notification.createdAt)}</time>
                </div>
              </div>
              <div className="notification-actions">
                {!notification.read && (
                  <button
                    type="button"
                    disabled={actionId === notification.id}
                    onClick={() => handleMarkRead(notification)}
                  >Mark as read</button>
                )}
                <button
                  className="notification-delete"
                  type="button"
                  disabled={actionId === notification.id}
                  onClick={() => handleDelete(notification)}
                >Delete</button>
              </div>
            </article>
          ))}
        </section>
      ) : (
        <section className="notifications-state notifications-empty">
          <span aria-hidden="true">♧</span>
          <h3>{filter === 'unread' ? 'You are all caught up' : 'No notifications yet'}</h3>
          <p>{filter === 'unread'
            ? 'Unread notifications will appear here when there are new business updates.'
            : 'Notifications will appear here when you add leads, schedule site visits or follow-ups, create deals, or upload documents.'}</p>
        </section>
      )}
    </main>
  );
}
