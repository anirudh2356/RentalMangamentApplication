import { useEffect, useState } from 'react';
import { fetchAnalyticsOverview } from '../../api';

const panel = {
  padding: 16,
  border: '1px solid #e6eaf0',
  borderRadius: 9,
  background: '#fff',
};
const muted = { margin: 0, color: '#718096', fontSize: 11 };

function money(value) {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(value ?? 0);
}

function titleCase(value) {
  return value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (character) => character.toUpperCase());
}

function Distribution({ title, counts, accent = '#4679b8' }) {
  const entries = Object.entries(counts || {});
  const max = Math.max(1, ...entries.map(([, count]) => count));
  return (
    <section style={panel}>
      <h3 style={{ margin: '0 0 14px', color: '#26374c', fontSize: 14 }}>{title}</h3>
      {entries.length ? (
        <div style={{ display: 'grid', gap: 11 }}>
          {entries.map(([name, count]) => (
            <div key={name}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, marginBottom: 5, color: '#627187', fontSize: 10 }}>
                <span>{titleCase(name)}</span><strong style={{ color: '#34455b' }}>{count}</strong>
              </div>
              <div aria-hidden="true" style={{ height: 7, overflow: 'hidden', borderRadius: 10, background: '#edf1f6' }}>
                <div style={{ width: `${Math.max(count ? 3 : 0, (count / max) * 100)}%`, height: '100%', borderRadius: 10, background: accent }} />
              </div>
            </div>
          ))}
        </div>
      ) : <p style={muted}>No records to summarize.</p>}
    </section>
  );
}

function SummaryCard({ label, value, detail, icon }) {
  return (
    <article style={{ ...panel, minHeight: 92 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'flex-start' }}>
        <div>
          <p style={muted}>{label}</p>
          <p style={{ margin: '8px 0 0', color: '#1f4d86', fontSize: 22, fontWeight: 700 }}>{value}</p>
          {detail && <p style={{ margin: '5px 0 0', color: '#8995a4', fontSize: 10 }}>{detail}</p>}
        </div>
        <span aria-hidden="true" style={{ display: 'grid', placeItems: 'center', width: 31, height: 31, borderRadius: 8, background: '#edf3fa', color: '#315f93', fontSize: 15 }}>{icon}</span>
      </div>
    </article>
  );
}

export default function Analytics() {
  const [overview, setOverview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function loadOverview() {
    setLoading(true);
    setError('');
    try {
      setOverview(await fetchAnalyticsOverview());
    } catch (requestError) {
      setError(requestError.message || 'Unable to load analytics.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadOverview();
  }, []);

  if (loading) {
    return <section role="status" style={{ ...panel, padding: 48, color: '#718096', textAlign: 'center', fontSize: 12 }}>Loading broker analytics…</section>;
  }

  if (error) {
    return (
      <section role="alert" style={{ ...panel, display: 'grid', justifyItems: 'center', gap: 12, padding: 42, color: '#718096', textAlign: 'center', fontSize: 12 }}>
        <strong style={{ color: '#a33636' }}>Analytics could not be loaded.</strong>
        <span>{error}</span>
        <button type="button" onClick={loadOverview} style={{ padding: '8px 12px', border: '1px solid #dfe5ec', borderRadius: 7, background: '#fff', color: '#315f93', cursor: 'pointer' }}>Try again</button>
      </section>
    );
  }

  const hasRecords = overview.totalLeads + overview.totalProperties + overview.activeDeals
    + overview.wonDeals + overview.lostDeals + overview.completedSiteVisits > 0;
  const conversion = overview.totalLeads
    ? `${((overview.wonLeads / overview.totalLeads) * 100).toFixed(1)}%`
    : '0%';

  return (
    <div style={{ display: 'grid', gap: 16, color: '#172033' }}>
      <header>
        <p style={{ margin: '0 0 3px', color: '#8492a5', fontSize: 9, fontWeight: 750, letterSpacing: 1 }}>BUSINESS INSIGHTS</p>
        <h2 style={{ margin: 0, color: '#1c2c40', fontSize: 23 }}>Analytics</h2>
        <p style={{ margin: '4px 0 0', color: '#718096', fontSize: 12 }}>Performance summaries based on your broker workspace data</p>
      </header>

      {!hasRecords && (
        <section role="status" style={{ ...panel, padding: 14, color: '#718096', fontSize: 11 }}>
          No analytics data yet. Add leads, properties, visits, or deals to see your business trends.
        </section>
      )}

      <section style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 175px), 1fr))', gap: 12 }} aria-label="Analytics key performance indicators">
        <SummaryCard label="Total Leads" value={overview.totalLeads} detail={`${overview.newLeads} new · ${overview.qualifiedLeads} qualified`} icon="◎" />
        <SummaryCard label="Lead Conversion" value={conversion} detail={`${overview.wonLeads} won of ${overview.totalLeads} leads`} icon="↗" />
        <SummaryCard label="Properties" value={overview.totalProperties} detail={`${overview.activeProperties} active · ${overview.soldRentedProperties} sold/rented`} icon="⌂" />
        <SummaryCard label="Upcoming Site Visits" value={overview.upcomingSiteVisits} detail={`${overview.completedSiteVisits} completed`} icon="▣" />
        <SummaryCard label="Deals" value={overview.activeDeals} detail={`${overview.wonDeals} won · ${overview.lostDeals} lost`} icon="◇" />
        <SummaryCard label="Total Deal Value" value={money(overview.totalDealValue)} detail="Across all your deals" icon="$" />
        <SummaryCard label="Total Commission" value={money(overview.totalCommission)} detail="From won deals" icon="＄" />
        <SummaryCard label="Pending Commission" value={money(overview.pendingCommission)} detail={overview.paymentTrackingSupported ? 'Awaiting payment' : 'Payment state is not tracked'} icon="◷" />
      </section>

      <section style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 290px), 1fr))', gap: 13 }}>
        <section style={panel} aria-label="Lead conversion summary">
          <h3 style={{ margin: '0 0 14px', color: '#26374c', fontSize: 14 }}>Lead conversion summary</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: 9 }}>
            {[
              ['New', overview.newLeads],
              ['Qualified', overview.qualifiedLeads],
              ['Won', overview.wonLeads],
            ].map(([label, value]) => (
              <div key={label} style={{ padding: 11, borderRadius: 7, background: '#f6f8fb' }}>
                <p style={muted}>{label}</p><strong style={{ display: 'block', marginTop: 6, color: '#34455b', fontSize: 18 }}>{value}</strong>
              </div>
            ))}
          </div>
          <p style={{ margin: '12px 0 0', color: '#718096', fontSize: 10 }}>{overview.lostLeads} leads marked lost</p>
        </section>
        <Distribution title="Lead status distribution" counts={overview.leadPipeline} />
        <Distribution title="Deal pipeline by stage" counts={overview.dealPipeline} accent="#6a8f63" />
      </section>

      <section style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 240px), 1fr))', gap: 13 }}>
        <Distribution title="Property listing summary" counts={overview.propertyStatusCounts} accent="#b18a4e" />
        <section style={panel}>
          <h3 style={{ margin: '0 0 14px', color: '#26374c', fontSize: 14 }}>Site visit summary</h3>
          <div style={{ display: 'grid', gap: 12 }}>
            {[['Upcoming', overview.upcomingSiteVisits], ['Completed', overview.completedSiteVisits]].map(([label, value]) => (
              <div key={label} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 11px', borderRadius: 7, background: '#f6f8fb', color: '#627187', fontSize: 11 }}>
                <span>{label}</span><strong style={{ color: '#34455b', fontSize: 15 }}>{value}</strong>
              </div>
            ))}
          </div>
        </section>
        <section style={panel}>
          <h3 style={{ margin: '0 0 14px', color: '#26374c', fontSize: 14 }}>Commission summary</h3>
          <div style={{ display: 'grid', gap: 11, color: '#627187', fontSize: 11 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 10 }}><span>Total from won deals</span><strong style={{ color: '#34455b' }}>{money(overview.totalCommission)}</strong></div>
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 10 }}><span>Pending</span><strong style={{ color: '#34455b' }}>{money(overview.pendingCommission)}</strong></div>
            <p style={{ margin: '2px 0 0', color: '#8995a4', fontSize: 10 }}>
              {overview.paymentTrackingSupported ? 'Paid commission is tracked.' : 'Payout status is not stored, so won-deal commissions are shown as pending.'}
            </p>
          </div>
        </section>
      </section>
    </div>
  );
}
