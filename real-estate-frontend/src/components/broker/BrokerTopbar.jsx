import { useEffect, useRef, useState } from 'react';
import { fetchUnreadNotificationCount, searchWorkspace } from '../../api';

const searchGroups = [
  ['LEAD', 'Leads'],
  ['PROPERTY', 'Properties'],
  ['DEAL', 'Deals'],
  ['SITE_VISIT', 'Site visits'],
  ['FOLLOW_UP', 'Follow-ups'],
  ['DOCUMENT', 'Documents'],
  ['TEAM_MEMBER', 'Team / Agents'],
];

export default function BrokerTopbar({
  title,
  user,
  onLogout,
  onMenuClick,
  activePage,
  onNavigate,
}) {
  const initial = user.name?.trim()?.charAt(0)?.toUpperCase() || 'B';
  const [unreadCount, setUnreadCount] = useState(null);
  const [query, setQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [searchError, setSearchError] = useState('');
  const [searchOpen, setSearchOpen] = useState(false);
  const searchContainerRef = useRef(null);
  const searchInputRef = useRef(null);

  useEffect(() => {
    const normalizedQuery = query.trim();
    if (normalizedQuery.length < 2 || !searchOpen) return undefined;

    let cancelled = false;
    const timeoutId = window.setTimeout(() => {
      setSearchLoading(true);
      setSearchError('');
      searchWorkspace(normalizedQuery)
        .then((response) => {
          if (!cancelled) setSearchResults(response.results || []);
        })
        .catch((error) => {
          if (!cancelled) {
            setSearchResults([]);
            setSearchError(error.message || 'Unable to search the workspace.');
          }
        })
        .finally(() => {
          if (!cancelled) setSearchLoading(false);
        });
    }, 250);

    return () => {
      cancelled = true;
      window.clearTimeout(timeoutId);
    };
  }, [query, searchOpen]);

  useEffect(() => {
    function handlePointerDown(event) {
      if (!searchContainerRef.current?.contains(event.target)) setSearchOpen(false);
    }

    function handleKeyDown(event) {
      if (event.key === 'Escape') setSearchOpen(false);
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        searchInputRef.current?.focus();
        if (query.trim().length >= 2) setSearchOpen(true);
      }
    }

    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [query]);

  function handleSearchChange(event) {
    const nextQuery = event.target.value;
    setQuery(nextQuery);
    setSearchResults([]);
    setSearchError('');
    setSearchOpen(nextQuery.trim().length >= 2);
  }

  function clearSearch() {
    setQuery('');
    setSearchResults([]);
    setSearchError('');
    setSearchLoading(false);
    setSearchOpen(false);
    searchInputRef.current?.focus();
  }

  function openResult(target) {
    onNavigate(target);
    clearSearch();
  }

  useEffect(() => {
    let mounted = true;
    const refreshCount = () => {
      fetchUnreadNotificationCount()
        .then((result) => {
          if (mounted) setUnreadCount(result.count);
        })
        .catch(() => {
          if (mounted) setUnreadCount(null);
        });
    };
    refreshCount();
    const intervalId = window.setInterval(refreshCount, 60000);
    window.addEventListener('broker-notifications-updated', refreshCount);
    return () => {
      mounted = false;
      window.clearInterval(intervalId);
      window.removeEventListener('broker-notifications-updated', refreshCount);
    };
  }, [activePage]);

  return (
    <header className="broker-topbar">
      <div className="broker-topbar-title">
        <button
          type="button"
          className="broker-menu-toggle"
          onClick={onMenuClick}
          aria-label="Open navigation"
        >
          ☰
        </button>
        <div>
          <p className="broker-topbar-eyebrow">BROKER WORKSPACE</p>
          <h1>{title}</h1>
        </div>
      </div>

      <div className="broker-topbar-actions">
        <div className="broker-search-container" ref={searchContainerRef}>
          <label className="broker-search">
            <span aria-hidden="true">⌕</span>
            <input
              ref={searchInputRef}
              type="search"
              placeholder="Search workspace"
              aria-label="Search workspace"
              aria-expanded={searchOpen && query.trim().length >= 2}
              aria-controls="broker-search-results"
              aria-autocomplete="list"
              value={query}
              onChange={handleSearchChange}
              onFocus={() => {
                if (query.trim().length >= 2) setSearchOpen(true);
              }}
            />
            {query && (
              <button
                type="button"
                className="broker-search-clear"
                onClick={clearSearch}
                aria-label="Clear search"
              >
                ×
              </button>
            )}
            <kbd>⌘ K</kbd>
          </label>
          {searchOpen && query.trim().length >= 2 && (
            <div
              id="broker-search-results"
              className="broker-search-dropdown"
              role="listbox"
              aria-label="Search results"
              aria-busy={searchLoading}
            >
              <strong className="broker-search-heading">Search results</strong>
              {searchLoading ? (
                <p className="broker-search-message" role="status">Searching…</p>
              ) : searchError ? (
                <p className="broker-search-message broker-search-error" role="alert">{searchError}</p>
              ) : searchResults.length === 0 ? (
                <p className="broker-search-message">No results found.</p>
              ) : searchGroups.map(([type, label]) => {
                const items = searchResults.filter((result) => result.type === type);
                if (!items.length) return null;
                return (
                  <section className="broker-search-group" key={type}>
                    <span>{label}</span>
                    {items.map((result) => (
                      <button
                        type="button"
                        className="broker-search-result"
                        key={`${type}-${result.id}`}
                        role="option"
                        aria-selected="false"
                        onClick={() => openResult(result.navigationTarget)}
                      >
                        <strong>{result.title}</strong>
                        {result.subtitle && <small>{result.subtitle}</small>}
                      </button>
                    ))}
                  </section>
                );
              })}
            </div>
          )}
        </div>

        <button
          type="button"
          className="broker-notifications-button"
          aria-label={unreadCount === null
            ? 'Notifications, unread count unavailable'
            : `Notifications, ${unreadCount} unread`}
          title="Notifications"
          aria-current={activePage === 'notifications' ? 'page' : undefined}
          onClick={() => onNavigate('notifications')}
        >
          ♧
          {unreadCount > 0 && <span className="broker-notifications-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>}
        </button>

        <div className="broker-profile">
          <span className="broker-avatar" aria-hidden="true">{initial}</span>
          <span className="broker-profile-copy">
            <strong>{user.name}</strong>
            <small>{user.role}</small>
          </span>
        </div>

        <button
          type="button"
          className="broker-topbar-logout"
          onClick={onLogout}
          aria-label="Log out"
        >
          Log out
        </button>
      </div>
    </header>
  );
}
