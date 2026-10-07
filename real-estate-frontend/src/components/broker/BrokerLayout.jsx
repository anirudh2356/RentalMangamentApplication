import { useState } from 'react';
import BrokerSidebar from './BrokerSidebar';
import BrokerTopbar from './BrokerTopbar';
import Chatbot from '../../Chatbot';
import './BrokerDashboard.css';
import './BrokerTheme.css';

const pageTitles = {
  overview: 'Dashboard overview',
  leads: 'Leads',
  properties: 'Properties',
  matching: 'Property matching',
  'site-visits': 'Site visits',
  deals: 'Deals',
  'follow-ups': 'Follow-ups',
  calendar: 'Calendar',
  commissions: 'Commissions',
  analytics: 'Analytics',
  reports: 'Reports',
  team: 'Team / Agents',
  documents: 'Documents',
  messages: 'Messages',
  reviews: 'Reviews',
  chatbot: 'Chatbot',
  notifications: 'Notifications',
  profile: 'Profile',
  settings: 'Settings',
};

export default function BrokerLayout({
  user,
  activePage,
  onNavigate,
  onLogout,
  theme = 'SYSTEM',
  children,
}) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [logoutError, setLogoutError] = useState('');

  async function handleLogout() {
    setLogoutError('');
    try {
      await onLogout();
    } catch (error) {
      setLogoutError(error.message || 'Unable to log out.');
    }
  }

  return (
    <div className="broker-shell" data-theme={theme}>
      <BrokerSidebar
        activePage={activePage}
        onNavigate={onNavigate}
        onLogout={handleLogout}
        mobileOpen={mobileOpen}
        onClose={() => setMobileOpen(false)}
      />
      <div className="broker-main">
        <BrokerTopbar
          title={pageTitles[activePage] || 'Dashboard'}
          user={user}
          onLogout={handleLogout}
          onMenuClick={() => setMobileOpen(true)}
          activePage={activePage}
          onNavigate={onNavigate}
        />
        {logoutError && (
          <div className="broker-alert" role="alert">{logoutError}</div>
        )}
        <main className="broker-content">{children}</main>
      </div>
      <Chatbot />
    </div>
  );
}
