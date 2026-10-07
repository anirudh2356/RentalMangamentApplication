import { useCallback, useEffect, useState } from 'react';
import {
  createDeal,
  deleteDeal,
  fetchCsrfToken,
  fetchDeal,
  fetchDeals,
  fetchLeads,
  fetchProperties,
  updateDeal,
} from '../../api';

const STAGES = ['NEW', 'SITE_VISIT', 'NEGOTIATION', 'DOCUMENTATION', 'CLOSED'];
const STATUSES = ['ACTIVE', 'WON', 'LOST', 'CANCELLED'];
const EMPTY_FORM = {
  leadId: '',
  propertyId: '',
  dealValue: '',
  commissionRate: '',
  stage: 'NEW',
  status: 'ACTIVE',
  expectedClosingDate: '',
  notes: '',
};
const inputStyle = {
  boxSizing: 'border-box',
  width: '100%',
  minHeight: 38,
  padding: '8px 10px',
  border: '1px solid #dfe5ec',
  borderRadius: 7,
  background: '#fff',
  color: '#26374c',
  font: 'inherit',
  fontSize: 12,
};
const primaryButton = {
  ...inputStyle,
  width: 'auto',
  borderColor: '#2459a6',
  background: '#2459a6',
  color: '#fff',
  fontWeight: 650,
  cursor: 'pointer',
};
const secondaryButton = {
  ...inputStyle,
  width: 'auto',
  background: '#fff',
  color: '#315f93',
  fontWeight: 600,
  cursor: 'pointer',
};
const panelStyle = {
  padding: 16,
  border: '1px solid #e6eaf0',
  borderRadius: 9,
  background: '#fff',
};

function titleCase(value) {
  return value?.replaceAll('_', ' ').toLowerCase()
    .replace(/\b\w/g, (character) => character.toUpperCase()) || '—';
}

function money(value) {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(value);
}

function DealForm({ deal, leads, properties, onCancel, onSave }) {
  const [form, setForm] = useState(deal ? {
    ...deal,
    leadId: String(deal.leadId),
    propertyId: String(deal.propertyId),
    expectedClosingDate: deal.expectedClosingDate || '',
  } : EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  function change(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    const payload = {
      ...form,
      leadId: Number(form.leadId),
      propertyId: Number(form.propertyId),
      dealValue: Number(form.dealValue),
      commissionRate: Number(form.commissionRate),
      expectedClosingDate: form.expectedClosingDate || null,
      notes: form.notes.trim() || null,
    };
    try {
      await onSave(payload);
    } catch (saveError) {
      setError(saveError.message || 'Unable to save this deal.');
    } finally {
      setSaving(false);
    }
  }

  const labelStyle = { display: 'grid', gap: 5, color: '#526176', fontSize: 11 };
  return (
    <div
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onCancel();
      }}
      style={{
        position: 'fixed',
        zIndex: 60,
        inset: 0,
        display: 'grid',
        placeItems: 'center',
        overflowY: 'auto',
        padding: 16,
        background: 'rgba(22, 34, 51, .48)',
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="deal-form-title"
        style={{
          width: 'min(100%, 660px)',
          maxHeight: '90vh',
          overflowY: 'auto',
          padding: 20,
          border: '1px solid #e4e8ee',
          borderRadius: 10,
          background: '#fff',
          boxShadow: '0 18px 60px rgba(18, 30, 48, .2)',
        }}
      >
        <header style={{ marginBottom: 16 }}>
          <p style={{ margin: 0, color: '#8492a5', fontSize: 9, fontWeight: 750, letterSpacing: 1 }}>TRANSACTION</p>
          <h2 id="deal-form-title" style={{ margin: '4px 0 0', color: '#1e3046', fontSize: 18 }}>
            {deal ? 'Edit Deal' : 'Add Deal'}
          </h2>
        </header>
        <form onSubmit={submit}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: 12 }}>
            <label style={labelStyle}>Lead
              <select name="leadId" value={form.leadId} onChange={change} style={inputStyle} required>
                <option value="">Select a lead</option>
                {leads.map((lead) => <option key={lead.id} value={lead.id}>{lead.name}</option>)}
              </select>
            </label>
            <label style={labelStyle}>Property
              <select name="propertyId" value={form.propertyId} onChange={change} style={inputStyle} required>
                <option value="">Select a property</option>
                {properties.map((property) => (
                  <option key={property.id} value={property.id}>{property.title} · {property.city}</option>
                ))}
              </select>
            </label>
            <label style={labelStyle}>Deal Value (₹)
              <input name="dealValue" type="number" min="0.01" step="0.01" value={form.dealValue} onChange={change} style={inputStyle} required />
            </label>
            <label style={labelStyle}>Commission Rate (%)
              <input name="commissionRate" type="number" min="0" max="100" step="0.01" value={form.commissionRate} onChange={change} style={inputStyle} required />
            </label>
            <label style={labelStyle}>Commission Amount (calculated)
              <input
                value={form.dealValue && form.commissionRate
                  ? money(Number(form.dealValue) * Number(form.commissionRate) / 100)
                  : 'Calculated when saved'}
                style={{ ...inputStyle, background: '#f7f8fa' }}
                readOnly
              />
            </label>
            <label style={labelStyle}>Expected Closing Date
              <input name="expectedClosingDate" type="date" value={form.expectedClosingDate || ''} onChange={change} style={inputStyle} />
            </label>
            <label style={labelStyle}>Stage
              <select name="stage" value={form.stage} onChange={change} style={inputStyle} required>
                {STAGES.map((stage) => <option key={stage} value={stage}>{titleCase(stage)}</option>)}
              </select>
            </label>
            <label style={labelStyle}>Status
              <select name="status" value={form.status} onChange={change} style={inputStyle} required>
                {STATUSES.map((status) => <option key={status} value={status}>{titleCase(status)}</option>)}
              </select>
            </label>
            <label style={{ ...labelStyle, gridColumn: '1 / -1' }}>Notes
              <textarea name="notes" value={form.notes || ''} onChange={change} style={{ ...inputStyle, resize: 'vertical' }} rows={4} maxLength={5000} />
            </label>
          </div>
          {error && <p role="alert" style={{ color: '#a33636', fontSize: 11 }}>{error}</p>}
          <footer style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 16 }}>
            <button type="button" style={secondaryButton} onClick={onCancel}>Cancel</button>
            <button type="submit" style={primaryButton} disabled={saving || !leads.length || !properties.length}>
              {saving ? 'Saving…' : deal ? 'Save Changes' : 'Add Deal'}
            </button>
          </footer>
        </form>
      </section>
    </div>
  );
}

export default function Deals({ csrfToken }) {
  const [deals, setDeals] = useState([]);
  const [leads, setLeads] = useState([]);
  const [properties, setProperties] = useState([]);
  const [search, setSearch] = useState('');
  const [stage, setStage] = useState('');
  const [status, setStatus] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [optionsError, setOptionsError] = useState('');
  const [success, setSuccess] = useState('');
  const [formDeal, setFormDeal] = useState(undefined);

  const loadDeals = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setDeals(await fetchDeals({ search, stage, status }));
    } catch (requestError) {
      setError(requestError.message || 'Unable to load deals.');
    } finally {
      setLoading(false);
    }
  }, [search, stage, status]);

  useEffect(() => {
    let cancelled = false;
    Promise.all([fetchLeads(), fetchProperties()])
      .then(([leadData, propertyData]) => {
        if (!cancelled) {
          setLeads(leadData);
          setProperties(propertyData);
        }
      })
      .catch((requestError) => {
        if (!cancelled) setOptionsError(requestError.message || 'Unable to load leads and properties.');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError('');
    fetchDeals({ search, stage, status })
      .then((data) => {
        if (!cancelled) setDeals(data);
      })
      .catch((requestError) => {
        if (!cancelled) setError(requestError.message || 'Unable to load deals.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [search, stage, status]);

  async function writeToken() {
    if (csrfToken) return csrfToken;
    return (await fetchCsrfToken()).token;
  }

  async function save(payload) {
    if (formDeal) {
      await updateDeal(formDeal.id, payload, await writeToken());
      setSuccess('Deal updated successfully.');
    } else {
      await createDeal(payload, await writeToken());
      setSuccess('Deal added successfully.');
    }
    setFormDeal(undefined);
    await loadDeals();
  }

  async function edit(deal) {
    setError('');
    try {
      setFormDeal(await fetchDeal(deal.id));
    } catch (requestError) {
      setError(requestError.message || 'Unable to load deal details.');
    }
  }

  async function remove(deal) {
    if (!window.confirm(`Delete the deal for "${deal.leadName}"?`)) return;
    setError('');
    setSuccess('');
    try {
      await deleteDeal(deal.id, await writeToken());
      setSuccess('Deal deleted.');
      await loadDeals();
    } catch (requestError) {
      setError(requestError.message || 'Unable to delete this deal.');
    }
  }

  return (
    <div style={{ display: 'grid', gap: 16, color: '#172033' }}>
      <header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 14, flexWrap: 'wrap' }}>
        <div>
          <p style={{ margin: '0 0 3px', color: '#8492a5', fontSize: 9, fontWeight: 750, letterSpacing: 1 }}>TRANSACTIONS</p>
          <h2 style={{ margin: 0, color: '#1c2c40', fontSize: 23 }}>Deals</h2>
          <p style={{ margin: '4px 0 0', color: '#718096', fontSize: 12 }}>Track your property transactions from offer to close</p>
        </div>
        <button type="button" style={primaryButton} onClick={() => setFormDeal(null)}>＋ Add Deal</button>
      </header>

      {success && <div role="status" style={{ padding: 10, borderRadius: 7, background: '#f1faf4', color: '#27643d', fontSize: 11 }}>{success}</div>}
      {error && <div role="alert" style={{ padding: 10, borderRadius: 7, background: '#fff5f5', color: '#a33636', fontSize: 11 }}>{error}</div>}
      {optionsError && <div role="alert" style={{ padding: 10, borderRadius: 7, background: '#fff5f5', color: '#a33636', fontSize: 11 }}>{optionsError}</div>}

      <section style={{ display: 'flex', flexWrap: 'wrap', gap: 9, padding: 12, border: '1px solid #e6eaf0', borderRadius: 9, background: '#fff' }} aria-label="Deal filters">
        <input
          type="search"
          aria-label="Search lead or property"
          placeholder="Search lead or property"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          style={{ ...inputStyle, minWidth: 190, flex: '1 1 230px' }}
        />
        <select aria-label="Filter by stage" value={stage} onChange={(event) => setStage(event.target.value)} style={{ ...inputStyle, width: 'auto', minWidth: 150 }}>
          <option value="">All stages</option>
          {STAGES.map((item) => <option key={item} value={item}>{titleCase(item)}</option>)}
        </select>
        <select aria-label="Filter by status" value={status} onChange={(event) => setStatus(event.target.value)} style={{ ...inputStyle, width: 'auto', minWidth: 140 }}>
          <option value="">All statuses</option>
          {STATUSES.map((item) => <option key={item} value={item}>{titleCase(item)}</option>)}
        </select>
      </section>

      {loading ? (
        <section role="status" style={{ ...panelStyle, padding: 42, color: '#718096', textAlign: 'center', fontSize: 12 }}>Loading deals…</section>
      ) : error ? (
        <section style={{ ...panelStyle, display: 'grid', justifyItems: 'center', gap: 10, padding: 40, color: '#718096', fontSize: 12 }}>
          Deals could not be loaded.
          <button type="button" style={secondaryButton} onClick={loadDeals}>Try again</button>
        </section>
      ) : deals.length ? (
        <section style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 320px), 1fr))', gap: 13 }} aria-label="Deals">
          {deals.map((deal) => (
            <article key={deal.id} style={panelStyle}>
              <header style={{ display: 'flex', justifyContent: 'space-between', gap: 10 }}>
                <div>
                  <h3 style={{ margin: 0, color: '#24354a', fontSize: 14 }}>{deal.leadName}</h3>
                  <p style={{ margin: '4px 0 0', color: '#66758a', fontSize: 11 }}>{deal.propertyTitle}</p>
                </div>
                <span style={{ alignSelf: 'start', padding: '5px 8px', borderRadius: 99, background: '#edf3fa', color: '#355f91', fontSize: 9, fontWeight: 700 }}>
                  {titleCase(deal.status)}
                </span>
              </header>
              <p style={{ margin: '12px 0 4px', color: '#1f4d86', fontSize: 16, fontWeight: 700 }}>{money(deal.dealValue)}</p>
              <p style={{ margin: 0, color: '#718096', fontSize: 10 }}>{deal.propertyLocation}</p>
              <dl style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '9px 12px', margin: '13px 0', fontSize: 10 }}>
                {[
                  ['Stage', titleCase(deal.stage)],
                  ['Commission', `${money(deal.commissionAmount)} (${deal.commissionRate}%)`],
                  ['Expected close', deal.expectedClosingDate ? new Date(`${deal.expectedClosingDate}T00:00:00`).toLocaleDateString() : 'Not set'],
                ].map(([term, value]) => (
                  <div key={term}><dt style={{ color: '#8995a4' }}>{term}</dt><dd style={{ margin: '3px 0 0', color: '#46566b' }}>{value}</dd></div>
                ))}
              </dl>
              {deal.notes && <p style={{ margin: '8px 0 0', color: '#637287', fontSize: 10 }}>{deal.notes}</p>}
              <footer style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 13, paddingTop: 10, borderTop: '1px solid #eef0f3' }}>
                <button type="button" style={{ ...secondaryButton, minHeight: 28, padding: '3px 8px' }} onClick={() => edit(deal)}>View / Edit</button>
                <button type="button" style={{ ...secondaryButton, minHeight: 28, padding: '3px 8px', color: '#a34848' }} onClick={() => remove(deal)}>Delete</button>
              </footer>
            </article>
          ))}
        </section>
      ) : search || stage || status ? (
        <section style={{ ...panelStyle, padding: 40, color: '#718096', textAlign: 'center', fontSize: 12 }}>No deals match your filters.</section>
      ) : (
        <section style={{ ...panelStyle, display: 'grid', justifyItems: 'center', gap: 9, padding: 40, textAlign: 'center' }}>
          <span aria-hidden="true" style={{ color: '#426a94', fontSize: 26 }}>◇</span>
          <h3 style={{ margin: 0, color: '#34455b', fontSize: 15 }}>No deals yet</h3>
          <p style={{ margin: 0, color: '#818da0', fontSize: 11 }}>Add a deal to track a transaction.</p>
          <button type="button" style={primaryButton} onClick={() => setFormDeal(null)}>Add Deal</button>
        </section>
      )}

      {formDeal !== undefined && (
        <DealForm
          deal={formDeal}
          leads={leads}
          properties={properties}
          onCancel={() => setFormDeal(undefined)}
          onSave={save}
        />
      )}
    </div>
  );
}
