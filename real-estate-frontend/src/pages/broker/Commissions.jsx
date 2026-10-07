import { useEffect, useMemo, useState } from 'react';
import { fetchCommissions } from '../../api';

const panelStyle = {
  padding: 16,
  border: '1px solid #e6eaf0',
  borderRadius: 9,
  background: '#fff',
};
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

function money(value) {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(value ?? 0);
}

function displayDate(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleDateString();
}

export default function Commissions() {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');

  async function loadCommissions() {
    setLoading(true);
    setError('');
    try {
      setSummary(await fetchCommissions());
    } catch (requestError) {
      setError(requestError.message || 'Unable to load commissions.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadCommissions();
  }, []);

  const rows = useMemo(() => {
    const query = search.trim().toLowerCase();
    const commissions = summary?.commissions || [];
    if (!query) return commissions;
    return commissions.filter((record) => [
      record.leadName,
      record.propertyTitle,
      record.propertyLocation,
    ].some((value) => value?.toLowerCase().includes(query)));
  }, [search, summary]);

  return (
    <div style={{ display: 'grid', gap: 16, color: '#172033' }}>
      <header>
        <p style={{ margin: '0 0 3px', color: '#8492a5', fontSize: 9, fontWeight: 750, letterSpacing: 1 }}>FINANCIALS</p>
        <h2 style={{ margin: 0, color: '#1c2c40', fontSize: 23 }}>Commissions</h2>
        <p style={{ margin: '4px 0 0', color: '#718096', fontSize: 12 }}>Commission earned from your won deals</p>
      </header>

      {error && (
        <div role="alert" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, padding: 11, borderRadius: 7, background: '#fff5f5', color: '#a33636', fontSize: 11 }}>
          <span>{error}</span>
          <button type="button" style={inputStyle} onClick={loadCommissions}>Try again</button>
        </div>
      )}

      <section style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))', gap: 12 }} aria-label="Commission totals">
        {[
          ['Total Commission', summary?.totalCommission],
          ['Pending Commission', summary?.pendingCommission],
          ['Paid Commission', summary?.paymentTrackingSupported ? summary?.paidCommission : null],
        ].map(([label, value]) => (
          <article key={label} style={panelStyle}>
            <p style={{ margin: 0, color: '#718096', fontSize: 11 }}>{label}</p>
            <p style={{ margin: '9px 0 0', color: '#1f4d86', fontSize: 21, fontWeight: 700 }}>
              {loading ? 'Loading…' : value == null && label === 'Paid Commission' ? 'Not tracked' : money(value)}
            </p>
            {label === 'Paid Commission' && !summary?.paymentTrackingSupported && !loading && (
              <small style={{ display: 'block', marginTop: 5, color: '#8995a4' }}>Payment state is not stored for deals.</small>
            )}
          </article>
        ))}
      </section>

      {!loading && !error && summary && !summary.paymentTrackingSupported && (
        <p style={{ margin: 0, color: '#718096', fontSize: 11 }}>
          Pending commission includes all won-deal commissions; paid amounts cannot be identified because deal payouts are not tracked.
        </p>
      )}

      <section style={{ ...panelStyle, display: 'flex', flexWrap: 'wrap', gap: 10 }} aria-label="Commission filters">
        <input
          type="search"
          aria-label="Search deal, property, or lead"
          placeholder="Search deal, property, or lead"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          style={{ ...inputStyle, minWidth: 220, flex: '1 1 240px' }}
        />
      </section>

      {loading ? (
        <section role="status" style={{ ...panelStyle, padding: 42, color: '#718096', textAlign: 'center', fontSize: 12 }}>Loading commissions…</section>
      ) : error ? (
        <section style={{ ...panelStyle, padding: 30, color: '#718096', textAlign: 'center', fontSize: 12 }}>Commission records could not be loaded.</section>
      ) : rows.length ? (
        <section style={{ ...panelStyle, overflowX: 'auto' }} aria-label="Won deal commissions">
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: 11 }}>
            <thead>
              <tr style={{ color: '#718096', borderBottom: '1px solid #e9edf2' }}>
                {['Lead', 'Property', 'Deal Value', 'Rate', 'Commission', 'Deal Status', 'Date'].map((heading) => (
                  <th key={heading} scope="col" style={{ padding: '10px 9px', fontWeight: 650, whiteSpace: 'nowrap' }}>{heading}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map((record) => (
                <tr key={record.dealId} style={{ borderBottom: '1px solid #f0f2f5', color: '#46566b' }}>
                  <td style={{ padding: '12px 9px', fontWeight: 650 }}>{record.leadName || '—'}</td>
                  <td style={{ padding: '12px 9px' }}>
                    <div style={{ color: '#26374c', fontWeight: 600 }}>{record.propertyTitle || '—'}</div>
                    <small style={{ color: '#8995a4' }}>{record.propertyLocation || ''}</small>
                  </td>
                  <td style={{ padding: '12px 9px', whiteSpace: 'nowrap' }}>{money(record.dealValue)}</td>
                  <td style={{ padding: '12px 9px', whiteSpace: 'nowrap' }}>{record.commissionRate}%</td>
                  <td style={{ padding: '12px 9px', color: '#1f4d86', fontWeight: 700, whiteSpace: 'nowrap' }}>{money(record.commissionAmount)}</td>
                  <td style={{ padding: '12px 9px' }}>{record.dealStatus}</td>
                  <td style={{ padding: '12px 9px', whiteSpace: 'nowrap' }}>{displayDate(record.date)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      ) : (
        <section style={{ ...panelStyle, display: 'grid', justifyItems: 'center', gap: 8, padding: 40, textAlign: 'center' }}>
          <span aria-hidden="true" style={{ color: '#426a94', fontSize: 26 }}>$</span>
          <h3 style={{ margin: 0, color: '#34455b', fontSize: 15 }}>{search ? 'No commissions match your search' : 'No commissions yet'}</h3>
          <p style={{ margin: 0, color: '#818da0', fontSize: 11 }}>
            {search ? 'Try a different lead or property search.' : 'Commissions from won deals will appear here.'}
          </p>
        </section>
      )}
    </div>
  );
}
