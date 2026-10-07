import { useEffect, useState } from 'react';
import { fetchTeamMembers } from '../../api';

const inputStyle = {
  boxSizing: 'border-box',
  minHeight: 38,
  padding: '8px 10px',
  border: '1px solid #dfe5ec',
  borderRadius: 7,
  background: '#fff',
  color: '#26374c',
  font: 'inherit',
  fontSize: 12,
};
const panelStyle = {
  padding: 16,
  border: '1px solid #e6eaf0',
  borderRadius: 9,
  background: '#fff',
};
const ROLES = ['BUYER', 'SELLER', 'BROKER'];

function humanize(value) {
  return value
    ? value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (character) => character.toUpperCase())
    : '—';
}

function memberInitials(name) {
  return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase();
}

export default function Team() {
  const [members, setMembers] = useState([]);
  const [search, setSearch] = useState('');
  const [role, setRole] = useState('');
  const [enabled, setEnabled] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [retryCount, setRetryCount] = useState(0);

  useEffect(() => {
    let cancelled = false;
    fetchTeamMembers({ search, role, enabled })
      .then((data) => {
        if (!cancelled) {
          setMembers(data);
          setError('');
        }
      })
      .catch((requestError) => {
        if (!cancelled) setError(requestError.message || 'Unable to load team members.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [search, role, enabled, retryCount]);

  const activeCount = members.filter((member) => member.enabled).length;

  return (
    <div style={{ display: 'grid', gap: 16, color: '#172033' }}>
      <header>
        <p style={{ margin: '0 0 3px', color: '#8492a5', fontSize: 9, fontWeight: 750, letterSpacing: 1 }}>PEOPLE</p>
        <h2 style={{ margin: 0, color: '#1c2c40', fontSize: 23 }}>Team / Agents</h2>
        <p style={{ margin: '4px 0 0', color: '#718096', fontSize: 12 }}>View the accounts associated with your broker team</p>
      </header>

      <section style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12 }} aria-label="Team totals">
        {[
          ['Team members', members.length],
          ['Enabled accounts', activeCount],
        ].map(([label, value]) => (
          <article key={label} style={panelStyle}>
            <p style={{ margin: 0, color: '#718096', fontSize: 11 }}>{label}</p>
            <p style={{ margin: '8px 0 0', color: '#1f4d86', fontSize: 22, fontWeight: 700 }}>{loading ? 'Loading…' : value}</p>
          </article>
        ))}
      </section>

      <section style={{ display: 'flex', flexWrap: 'wrap', gap: 9, padding: 12, border: '1px solid #e6eaf0', borderRadius: 9, background: '#fff' }} aria-label="Team filters">
        <input
          type="search"
          aria-label="Search team members"
          placeholder="Search name or email"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          style={{ ...inputStyle, minWidth: 190, flex: '1 1 230px' }}
        />
        <select aria-label="Filter by role" value={role} onChange={(event) => setRole(event.target.value)} style={{ ...inputStyle, width: 'auto', minWidth: 140 }}>
          <option value="">All roles</option>
          {ROLES.map((item) => <option key={item} value={item}>{humanize(item)}</option>)}
        </select>
        <select aria-label="Filter by account status" value={enabled} onChange={(event) => setEnabled(event.target.value)} style={{ ...inputStyle, width: 'auto', minWidth: 150 }}>
          <option value="">All account statuses</option>
          <option value="true">Enabled</option>
          <option value="false">Disabled</option>
        </select>
      </section>

      {error && (
        <div role="alert" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, padding: 11, borderRadius: 7, background: '#fff5f5', color: '#a33636', fontSize: 11 }}>
          <span>{error}</span>
          <button type="button" style={inputStyle} onClick={() => setRetryCount((count) => count + 1)}>Try again</button>
        </div>
      )}

      {loading ? (
        <section role="status" style={{ ...panelStyle, padding: 42, color: '#718096', textAlign: 'center', fontSize: 12 }}>Loading team members…</section>
      ) : error ? (
        <section style={{ ...panelStyle, padding: 30, color: '#718096', textAlign: 'center', fontSize: 12 }}>Team members could not be loaded.</section>
      ) : members.length ? (
        <section style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 280px), 1fr))', gap: 13 }} aria-label="Team members">
          {members.map((member) => (
            <article key={member.id} style={panelStyle}>
              <header style={{ display: 'flex', alignItems: 'center', gap: 11 }}>
                <span aria-hidden="true" style={{ display: 'grid', flex: '0 0 auto', width: 40, height: 40, placeItems: 'center', borderRadius: '50%', background: '#edf3fa', color: '#315f93', fontSize: 12, fontWeight: 750 }}>
                  {memberInitials(member.name)}
                </span>
                <div style={{ minWidth: 0, flex: 1 }}>
                  <h3 style={{ overflow: 'hidden', margin: 0, color: '#24354a', fontSize: 14, textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{member.name}</h3>
                  <p style={{ overflow: 'hidden', margin: '4px 0 0', color: '#66758a', fontSize: 11, textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{member.email || 'No email provided'}</p>
                </div>
                <span style={{ padding: '5px 8px', borderRadius: 99, background: member.enabled ? '#eef8f0' : '#f4f5f7', color: member.enabled ? '#27643d' : '#69778a', fontSize: 9, fontWeight: 700 }}>
                  {member.enabled ? 'Enabled' : 'Disabled'}
                </span>
              </header>
              <dl style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '10px 12px', margin: '15px 0 0', paddingTop: 12, borderTop: '1px solid #eef0f3', fontSize: 10 }}>
                <div>
                  <dt style={{ color: '#8995a4' }}>Role</dt>
                  <dd style={{ margin: '3px 0 0', color: '#46566b' }}>{humanize(member.role)}</dd>
                </div>
                <div>
                  <dt style={{ color: '#8995a4' }}>Created</dt>
                  <dd style={{ margin: '3px 0 0', color: '#46566b' }}>
                    {member.createdAt ? new Date(member.createdAt).toLocaleDateString() : '—'}
                  </dd>
                </div>
              </dl>
            </article>
          ))}
        </section>
      ) : (
        <section style={{ ...panelStyle, display: 'grid', justifyItems: 'center', gap: 9, padding: 40, textAlign: 'center' }}>
          <span aria-hidden="true" style={{ color: '#426a94', fontSize: 26 }}>♧</span>
          <h3 style={{ margin: 0, color: '#34455b', fontSize: 15 }}>{search || role || enabled ? 'No team members match your filters' : 'No team members assigned'}</h3>
          <p style={{ maxWidth: 420, margin: 0, color: '#818da0', fontSize: 11, lineHeight: 1.6 }}>
            {search || role || enabled
              ? 'Try changing or clearing your filters.'
              : 'Team membership is read-only here. Accounts must be associated with this broker through the existing secure account provisioning process.'}
          </p>
        </section>
      )}
    </div>
  );
}
