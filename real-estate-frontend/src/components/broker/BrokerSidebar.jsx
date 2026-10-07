const navigationGroups = [
  {
    label: 'WORKSPACE',
    items: [
      ['overview', 'Overview', '◫'],
      ['leads', 'Leads', '◎'],
      ['properties', 'Properties', '⌂'],
      ['matching', 'Property Matching', '⇄'],
      ['site-visits', 'Site Visits', '▣'],
      ['deals', 'Deals', '◇'],
      ['follow-ups', 'Follow-ups', '↗'],
      ['calendar', 'Calendar', '▦'],
      ['commissions', 'Commissions', '$'],
      ['analytics', 'Analytics', '⌁'],
      ['reports', 'Reports', '▥'],
      ['team', 'Team / Agents', '♧'],
      ['documents', 'Documents', '▤'],
    ],
  },
  {
    label: 'COMMUNICATION',
    items: [
      ['messages', 'Messages', '□'],
      ['reviews', 'Reviews', '☆'],
      ['chatbot', 'Chatbot', '✦'],
      ['notifications', 'Notifications', '♧'],
    ],
  },
  {
    label: 'ACCOUNT',
    items: [
      ['profile', 'Profile', '◉'],
      ['settings', 'Settings', '⚙'],
    ],
  },
];

export default function BrokerSidebar({
  activePage,
  onNavigate,
  onLogout,
  mobileOpen,
  onClose,
}) {
  return (
    <>
      {mobileOpen && (
        <button
          type="button"
          className="broker-sidebar-backdrop"
          aria-label="Close navigation"
          onClick={onClose}
        />
      )}
      <aside className={`broker-sidebar${mobileOpen ? ' is-open' : ''}`}>
        <div className="broker-sidebar-brand">
          <span className="broker-brand-mark">RE</span>
          <span className="broker-brand-copy">
            <strong>EstateFlow</strong>
            <small>Broker workspace</small>
          </span>
          <button
            type="button"
            className="broker-mobile-close"
            onClick={onClose}
            aria-label="Close navigation"
          >
            ×
          </button>
        </div>

        <nav className="broker-navigation" aria-label="Broker navigation">
          {navigationGroups.map((group) => (
            <div className="broker-nav-group" key={group.label}>
              <p className="broker-nav-heading">{group.label}</p>
              {group.items.map(([id, label, icon]) => (
                <button
                  className={`broker-nav-item${activePage === id ? ' active' : ''}`}
                  type="button"
                  key={id}
                  aria-current={activePage === id ? 'page' : undefined}
                  onClick={() => {
                    onNavigate(id);
                    onClose();
                  }}
                >
                  <span className="broker-nav-icon" aria-hidden="true">{icon}</span>
                  <span>{label}</span>
                </button>
              ))}
            </div>
          ))}
        </nav>

        <div className="broker-sidebar-footer">
          <p>Manage your business in one place.</p>
          <button type="button" className="broker-logout-button" onClick={onLogout}>
            <span aria-hidden="true">↪</span>
            Log out
          </button>
        </div>
      </aside>
    </>
  );
}
