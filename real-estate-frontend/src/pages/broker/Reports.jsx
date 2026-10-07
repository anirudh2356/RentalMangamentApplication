import { useEffect, useMemo, useState } from 'react';
import { downloadReportCsv, fetchReport } from '../../api';
import './Reports.css';

const reportOptions = [
  ['overview', 'Overview Report', false],
  ['leads', 'Lead Report', true],
  ['properties', 'Property Report', true],
  ['site-visits', 'Site Visit Report', true],
  ['deals', 'Sales / Deal Report', true],
  ['commissions', 'Commission Report', false],
];

const statusOptions = {
  leads: ['NEW', 'CONTACTED', 'QUALIFIED', 'SITE_VISIT', 'NEGOTIATION', 'WON', 'LOST'],
  properties: ['ACTIVE', 'INACTIVE', 'SOLD', 'RENTED'],
  'site-visits': ['SCHEDULED', 'RESCHEDULED', 'COMPLETED', 'CANCELLED'],
  deals: ['ACTIVE', 'WON', 'LOST', 'CANCELLED'],
};

const currency = (value) => new Intl.NumberFormat(undefined, {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 2,
}).format(value ?? 0);

function pretty(value) {
  return value
    ? value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase())
    : '—';
}

function formatDate(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString();
}

function recordsFor(reportType, report) {
  if (reportType === 'leads') return report.leads || [];
  if (reportType === 'properties') return report.properties || [];
  if (reportType === 'site-visits') return report.visits || [];
  if (reportType === 'deals') return report.deals || [];
  if (reportType === 'commissions') return report.commissions || [];
  return [];
}

function ReportTable({ reportType, rows }) {
  let columns = [];
  if (reportType === 'leads') {
    columns = [['name', 'Lead'], ['source', 'Source'], ['status', 'Status'], ['interestedProperty', 'Property'], ['createdAt', 'Created']];
  } else if (reportType === 'properties') {
    columns = [['title', 'Property'], ['propertyType', 'Type'], ['listingType', 'Listing'], ['status', 'Status'], ['price', 'Price'], ['city', 'City']];
  } else if (reportType === 'site-visits') {
    columns = [['scheduledAt', 'Scheduled'], ['status', 'Status'], ['propertyTitle', 'Property'], ['leadName', 'Lead'], ['notes', 'Details']];
  } else if (reportType === 'deals') {
    columns = [['leadName', 'Lead'], ['propertyTitle', 'Property'], ['dealValue', 'Deal Value'], ['stage', 'Stage'], ['status', 'Status'], ['createdAt', 'Created']];
  } else {
    columns = [['leadName', 'Lead'], ['propertyTitle', 'Property'], ['dealValue', 'Deal Value'], ['commissionRate', 'Rate'], ['commissionAmount', 'Commission'], ['date', 'Date']];
  }

  function display(key, value) {
    if (value == null || value === '') return '—';
    if (['createdAt', 'scheduledAt', 'date'].includes(key)) return formatDate(value);
    if (['price', 'dealValue', 'commissionAmount'].includes(key)) return currency(value);
    if (key === 'commissionRate') return `${value}%`;
    if (typeof value === 'boolean') return value ? 'Yes' : 'No';
    if (['source', 'status', 'stage', 'propertyType', 'listingType'].includes(key)) return pretty(value);
    return value;
  }

  return (
    <div className="reports-table-scroll">
      <table className="reports-table">
        <thead><tr>{columns.map(([, label]) => <th key={label} scope="col">{label}</th>)}</tr></thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={row.id || row.dealId || `${reportType}-${index}`}>
              {columns.map(([key]) => <td key={key}>{display(key, row[key])}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Breakdown({ title, values, formatValue = (value) => value }) {
  const entries = Object.entries(values || {});
  if (!entries.length) return null;
  const max = Math.max(1, ...entries.map(([, value]) => Number(value) || 0));
  return (
    <section className="reports-panel">
      <h3>{title}</h3>
      <div className="reports-breakdown">
        {entries.map(([label, value]) => (
          <div className="reports-breakdown-row" key={label}>
            <div className="reports-breakdown-label"><span>{pretty(label)}</span><strong>{formatValue(value)}</strong></div>
            <div className="reports-bar-track" aria-hidden="true">
              <span style={{ width: `${Math.max(0, (Number(value) / max) * 100)}%` }} />
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}

function SummaryCards({ entries }) {
  return (
    <section className="reports-summary-grid" aria-label="Report summary">
      {entries.map(([label, value]) => (
        <article className="reports-summary-card" key={label}>
          <span>{label}</span>
          <strong>{value}</strong>
        </article>
      ))}
    </section>
  );
}

function ReportContent({ reportType, report }) {
  const rows = recordsFor(reportType, report);

  if (reportType === 'overview') {
    return (
      <SummaryCards entries={[
        ['Total Leads', report.totalLeads],
        ['New Leads', report.newLeads],
        ['Qualified Leads', report.qualifiedLeads],
        ['Active Properties', report.activeProperties],
        ['Upcoming Site Visits', report.upcomingSiteVisits],
        ['Completed Site Visits', report.completedSiteVisits],
        ['Active Deals', report.activeDeals],
        ['Won Deals', report.wonDeals],
        ['Lost Deals', report.lostDeals],
        ['Total Deal Value', currency(report.totalDealValue)],
        ['Total Commission', currency(report.totalCommission)],
        ['Pending Commission', currency(report.pendingCommission)],
      ]} />
    );
  }

  let summaries;
  const breakdowns = [];
  if (reportType === 'leads') {
    summaries = [['Total Leads', report.totalLeads]];
    breakdowns.push(['Leads by Status', report.byStatus], ['Leads by Source', report.bySource]);
  } else if (reportType === 'properties') {
    summaries = [
      ['Total Properties', report.totalProperties],
      ['Active', report.activeProperties],
      ['Available', report.availableProperties],
      ['Total Value', currency(report.totalPropertyValue)],
      ['Average Value', currency(report.averagePropertyValue)],
    ];
    breakdowns.push(['Properties by Type', report.byType], ['Listing Type', report.byListingType], ['Property Status', report.byStatus]);
  } else if (reportType === 'site-visits') {
    summaries = [
      ['Total Visits', report.totalVisits],
      ['Upcoming', report.upcomingVisits],
      ['Completed', report.completedVisits],
      ['Cancelled', report.cancelledVisits],
    ];
    breakdowns.push(['Visits by Date', report.byDate], ['Visits by Property', report.byProperty], ['Visits by Lead', report.byLead]);
  } else if (reportType === 'deals') {
    summaries = [
      ['Total Deals', report.totalDeals],
      ['Active', report.activeDeals],
      ['Won', report.wonDeals],
      ['Lost', report.lostDeals],
      ['Total Deal Value', currency(report.totalDealValue)],
      ['Average Deal Value', currency(report.averageDealValue)],
    ];
    breakdowns.push(['Deals by Stage', report.byStage], ['Deals by Status', report.byStatus], ['Deals by Date', report.byDate]);
  } else {
    summaries = [
      ['Total Commission', currency(report.totalCommission)],
      ['Pending Commission', currency(report.pendingCommission)],
      ['Commission Records', rows.length],
    ];
    breakdowns.push(['Commission by Date', report.byDate], ['Commissions by Deal', Object.fromEntries(
      rows.map((record) => [`Deal #${record.dealId}`, record.commissionAmount]),
    )]);
  }

  return (
    <>
      <SummaryCards entries={summaries} />
      <div className="reports-breakdown-grid">
        {breakdowns.map(([title, values]) => (
          <Breakdown
            key={title}
            title={title}
            values={values}
            formatValue={reportType === 'commissions' ? currency : undefined}
          />
        ))}
      </div>
      {rows.length > 0 && (
        <section className="reports-panel">
          <h3>{reportOptions.find(([key]) => key === reportType)?.[1].replace(' Report', '')} Records</h3>
          <ReportTable reportType={reportType} rows={rows} />
        </section>
      )}
      {rows.length === 0 && <div className="reports-empty">There are no records for this report and filter combination.</div>}
    </>
  );
}

export default function Reports() {
  const [reportType, setReportType] = useState('overview');
  const [draftFilters, setDraftFilters] = useState({ from: '', to: '', status: '' });
  const [filters, setFilters] = useState({});
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [exporting, setExporting] = useState(false);
  const [exportMessage, setExportMessage] = useState('');
  const [retry, setRetry] = useState(0);
  const canFilterStatus = Boolean(statusOptions[reportType]);

  useEffect(() => {
    let cancelled = false;
    fetchReport(reportType, filters)
      .then((data) => {
        if (!cancelled) {
          setReport(data);
          setError('');
        }
      })
      .catch((requestError) => {
        if (!cancelled) {
          setReport(null);
          setError(requestError.message || 'Unable to load this report.');
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [reportType, filters, retry]);

  const rows = useMemo(() => recordsFor(reportType, report || {}), [reportType, report]);

  function updateDraft(key, value) {
    setDraftFilters((current) => ({ ...current, [key]: value }));
  }

  function applyFilters(event) {
    event.preventDefault();
    if (draftFilters.from && draftFilters.to && draftFilters.from > draftFilters.to) {
      setError('The start date must be on or before the end date.');
      return;
    }
    setLoading(true);
    setError('');
    setExportMessage('');
    setFilters({
      ...(draftFilters.from ? { from: draftFilters.from } : {}),
      ...(draftFilters.to ? { to: draftFilters.to } : {}),
      ...(canFilterStatus && draftFilters.status ? { status: draftFilters.status } : {}),
    });
  }

  function resetFilters() {
    setLoading(true);
    setDraftFilters({ from: '', to: '', status: '' });
    setFilters({});
    setError('');
    setExportMessage('');
  }

  async function exportCsv() {
    setExporting(true);
    setExportMessage('');
    setError('');
    try {
      const file = await downloadReportCsv(reportType, filters);
      const url = URL.createObjectURL(file);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${reportType}-report.csv`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
      setExportMessage('CSV export downloaded.');
    } catch (requestError) {
      setError(requestError.message || 'Unable to export this report.');
    } finally {
      setExporting(false);
    }
  }

  return (
    <main className="broker-reports">
      <header className="reports-heading">
        <div>
          <p className="reports-eyebrow">BUSINESS INSIGHTS</p>
          <h2>Reports</h2>
          <p>Explore and export reports based on your broker workspace data.</p>
        </div>
        {reportType !== 'overview' && (
          <button className="reports-export" type="button" onClick={exportCsv} disabled={exporting || loading || !report}>
            {exporting ? 'Preparing CSV…' : 'Export CSV'}
          </button>
        )}
      </header>

      <form className="reports-filters" onSubmit={applyFilters}>
        <label>
          Report Type
          <select
            value={reportType}
            onChange={(event) => {
              const nextType = event.target.value;
              setReportType(nextType);
              setLoading(true);
              setError('');
              setDraftFilters((current) => ({ ...current, status: '' }));
              setFilters((current) => ({ from: current.from, to: current.to }));
              setError('');
            }}
          >
            {reportOptions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </label>
        <label>
          From
          <input type="date" value={draftFilters.from} onChange={(event) => updateDraft('from', event.target.value)} />
        </label>
        <label>
          To
          <input type="date" value={draftFilters.to} onChange={(event) => updateDraft('to', event.target.value)} />
        </label>
        {canFilterStatus && (
          <label>
            Status
            <select value={draftFilters.status} onChange={(event) => updateDraft('status', event.target.value)}>
              <option value="">All statuses</option>
              {statusOptions[reportType].map((status) => <option key={status} value={status}>{pretty(status)}</option>)}
            </select>
          </label>
        )}
        <div className="reports-filter-actions">
          <button className="reports-apply" type="submit">Apply Filters</button>
          <button className="reports-reset" type="button" onClick={resetFilters}>Reset</button>
        </div>
      </form>

      {error && (
        <div className="reports-error" role="alert">
          <span>{error}</span>
          <button type="button"           onClick={() => {
            setLoading(true);
            setError('');
            setRetry((count) => count + 1);
          }}>Retry</button>
        </div>
      )}
      {exportMessage && <p className="reports-success" role="status">{exportMessage}</p>}

      {loading ? (
        <div className="reports-state" role="status">Loading report…</div>
      ) : error ? (
        <div className="reports-state">The report could not be loaded.</div>
      ) : report ? (
        <ReportContent reportType={reportType} report={report} />
      ) : (
        <div className="reports-empty">
          {rows.length === 0 ? 'No report data is available for the selected filters.' : 'No report data available.'}
        </div>
      )}
    </main>
  );
}
