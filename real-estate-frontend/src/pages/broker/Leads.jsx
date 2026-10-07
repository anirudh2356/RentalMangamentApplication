import { useCallback, useEffect, useState } from 'react';
import {
  createLead,
  deleteLead,
  fetchCsrfToken,
  fetchLeads,
  fetchProperties,
  updateLead,
} from '../../api';

const STATUSES = ['NEW', 'CONTACTED', 'QUALIFIED', 'SITE_VISIT', 'NEGOTIATION', 'WON', 'LOST'];
const SOURCES = ['WEBSITE', 'PHONE', 'REFERRAL', 'SOCIAL_MEDIA', 'WALK_IN', 'OTHER'];
const EMPTY_FORM = {
  name: '',
  email: '',
  phone: '',
  source: 'WEBSITE',
  status: 'NEW',
  interestedPropertyId: '',
  budgetMin: '',
  budgetMax: '',
  preferredLocation: '',
  notes: '',
};

const pageStyle = { display: 'grid', gap: 16, color: '#172033' };
const controlStyle = {
  minHeight: 38,
  padding: '8px 10px',
  border: '1px solid #dfe5ec',
  borderRadius: 7,
  background: '#fff',
  color: '#26374c',
  font: 'inherit',
  fontSize: 12,
};
const primaryButtonStyle = {
  ...controlStyle,
  borderColor: '#2459a6',
  background: '#2459a6',
  color: '#fff',
  fontWeight: 650,
  cursor: 'pointer',
};
const secondaryButtonStyle = {
  ...controlStyle,
  color: '#315f93',
  fontWeight: 600,
  cursor: 'pointer',
};

function humanize(value) {
  return value
    ? value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (char) => char.toUpperCase())
    : '—';
}

function formatBudget(min, max) {
  const amount = (value) => new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(value);
  if (min != null && max != null) return `${amount(min)} – ${amount(max)}`;
  if (min != null) return `From ${amount(min)}`;
  if (max != null) return `Up to ${amount(max)}`;
  return 'Not specified';
}

function LeadForm({ lead, properties, onCancel, onSave }) {
  const editing = Boolean(lead);
  const [form, setForm] = useState(lead ? {
    ...EMPTY_FORM,
    ...lead,
    interestedPropertyId: lead.interestedPropertyId ?? '',
    budgetMin: lead.budgetMin ?? '',
    budgetMax: lead.budgetMax ?? '',
  } : EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');

  function change(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    setFormError('');
    const payload = {
      ...form,
      email: form.email.trim() || null,
      phone: form.phone.trim() || null,
      interestedPropertyId: form.interestedPropertyId ? Number(form.interestedPropertyId) : null,
      budgetMin: form.budgetMin === '' ? null : Number(form.budgetMin),
      budgetMax: form.budgetMax === '' ? null : Number(form.budgetMax),
      preferredLocation: form.preferredLocation.trim() || null,
      notes: form.notes.trim() || null,
    };
    try {
      await onSave(payload);
    } catch (error) {
      setFormError(error.message || 'Unable to save this lead.');
    } finally {
      setSaving(false);
    }
  }

  const fieldStyle = { display: 'grid', gap: 5, fontSize: 11, color: '#526176' };
  const inputStyle = { ...controlStyle, width: '100%', boxSizing: 'border-box' };

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
        aria-labelledby="lead-form-title"
        style={{
          width: 'min(100%, 680px)',
          maxHeight: '90vh',
          overflowY: 'auto',
          padding: 20,
          border: '1px solid #e4e8ee',
          borderRadius: 10,
          background: '#fff',
          boxShadow: '0 18px 60px rgba(18, 30, 48, .2)',
        }}
      >
        <header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
          <div>
            <p style={{ margin: 0, color: '#8492a5', fontSize: 9, fontWeight: 750, letterSpacing: 1 }}>
              CUSTOMER ENQUIRY
            </p>
            <h2 id="lead-form-title" style={{ margin: '4px 0 0', fontSize: 18, color: '#1e3046' }}>
              {editing ? 'Edit Lead' : 'Add Lead'}
            </h2>
          </div>
          <button type="button" onClick={onCancel} aria-label="Close" style={secondaryButtonStyle}>×</button>
        </header>
        <form onSubmit={submit}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: 12 }}>
            <label style={fieldStyle}>Name
              <input style={inputStyle} name="name" value={form.name} onChange={change} maxLength={120} required />
            </label>
            <label style={fieldStyle}>Email
              <input style={inputStyle} type="email" name="email" value={form.email || ''} onChange={change} maxLength={254} />
            </label>
            <label style={fieldStyle}>Phone
              <input style={inputStyle} type="tel" name="phone" value={form.phone || ''} onChange={change} maxLength={30} />
            </label>
            <label style={fieldStyle}>Source
              <select style={inputStyle} name="source" value={form.source} onChange={change} required>
                {SOURCES.map((source) => <option key={source} value={source}>{humanize(source)}</option>)}
              </select>
            </label>
            <label style={fieldStyle}>Status
              <select style={inputStyle} name="status" value={form.status} onChange={change} required>
                {STATUSES.map((status) => <option key={status} value={status}>{humanize(status)}</option>)}
              </select>
            </label>
            <label style={fieldStyle}>Interested Property
              <select
                style={inputStyle}
                name="interestedPropertyId"
                value={form.interestedPropertyId}
                onChange={change}
              >
                <option value="">No property selected</option>
                {properties.map((property) => (
                  <option key={property.id} value={property.id}>{property.title} · {property.city}</option>
                ))}
              </select>
            </label>
            <label style={fieldStyle}>Minimum Budget (₹)
              <input style={inputStyle} type="number" min="0" step="0.01" name="budgetMin" value={form.budgetMin} onChange={change} />
            </label>
            <label style={fieldStyle}>Maximum Budget (₹)
              <input style={inputStyle} type="number" min="0" step="0.01" name="budgetMax" value={form.budgetMax} onChange={change} />
            </label>
            <label style={fieldStyle}>Preferred Location
              <input style={inputStyle} name="preferredLocation" value={form.preferredLocation || ''} onChange={change} maxLength={255} />
            </label>
            <label style={{ ...fieldStyle, gridColumn: '1 / -1' }}>Notes
              <textarea style={{ ...inputStyle, resize: 'vertical' }} name="notes" value={form.notes || ''} onChange={change} rows={4} maxLength={5000} />
            </label>
          </div>
          {formError && <p role="alert" style={{ color: '#a33636', fontSize: 11 }}>{formError}</p>}
          <footer style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 16 }}>
            <button type="button" style={secondaryButtonStyle} onClick={onCancel}>Cancel</button>
            <button type="submit" style={primaryButtonStyle} disabled={saving}>
              {saving ? 'Saving…' : editing ? 'Save Changes' : 'Add Lead'}
            </button>
          </footer>
        </form>
      </section>
    </div>
  );
}

export default function Leads({ csrfToken }) {
  const [leads, setLeads] = useState([]);
  const [properties, setProperties] = useState([]);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [source, setSource] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [modalLead, setModalLead] = useState(undefined);

  const loadLeads = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setLeads(await fetchLeads({ search, status, source }));
    } catch (requestError) {
      setError(requestError.message || 'Unable to load leads.');
    } finally {
      setLoading(false);
    }
  }, [search, status, source]);

  useEffect(() => {
    let cancelled = false;
    fetchProperties()
      .then((data) => {
        if (!cancelled) setProperties(data);
      })
      .catch((requestError) => {
        if (!cancelled) setError(requestError.message || 'Unable to load your properties.');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError('');
    fetchLeads({ search, status, source })
      .then((data) => {
        if (!cancelled) setLeads(data);
      })
      .catch((requestError) => {
        if (!cancelled) setError(requestError.message || 'Unable to load leads.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [search, status, source]);

  async function tokenForWrite() {
    if (csrfToken) return csrfToken;
    const csrf = await fetchCsrfToken();
    return csrf.token;
  }

  async function saveLead(payload) {
    const token = await tokenForWrite();
    if (modalLead) {
      await updateLead(modalLead.id, payload, token);
      setSuccess('Lead updated successfully.');
    } else {
      await createLead(payload, token);
      setSuccess('Lead added successfully.');
    }
    setModalLead(undefined);
    await loadLeads();
  }

  async function editLead(lead) {
    setError('');
    try {
      const details = await fetchLeads({ search: lead.email || lead.phone || lead.name });
      setModalLead(details.find((item) => item.id === lead.id) || lead);
    } catch (requestError) {
      setError(requestError.message || 'Unable to load lead details.');
    }
  }

  async function deleteLeadRecord(lead) {
    if (!window.confirm(`Delete the lead for "${lead.name}"? This cannot be undone.`)) return;
    setError('');
    setSuccess('');
    try {
      await deleteLead(lead.id, await tokenForWrite());
      setSuccess('Lead deleted successfully.');
      await loadLeads();
    } catch (requestError) {
      setError(requestError.message || 'Unable to delete this lead.');
    }
  }

  const searchStyle = { ...controlStyle, minWidth: 190, flex: '1 1 230px' };

  return (
    <div style={pageStyle}>
      <header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 14, flexWrap: 'wrap' }}>
        <div>
          <p style={{ margin: '0 0 3px', color: '#8492a5', fontSize: 9, fontWeight: 750, letterSpacing: 1 }}>
            CUSTOMER RELATIONSHIPS
          </p>
          <h2 style={{ margin: 0, color: '#1c2c40', fontSize: 23 }}>Leads</h2>
          <p style={{ margin: '4px 0 0', color: '#718096', fontSize: 12 }}>
            Manage your buyer and customer enquiries
          </p>
        </div>
        <button type="button" style={primaryButtonStyle} onClick={() => setModalLead(null)}>
          ＋ Add Lead
        </button>
      </header>

      {success && <div role="status" style={{ padding: 10, borderRadius: 7, background: '#f1faf4', color: '#27643d', fontSize: 11 }}>{success}</div>}
      {error && <div role="alert" style={{ padding: 10, borderRadius: 7, background: '#fff5f5', color: '#a33636', fontSize: 11 }}>{error}</div>}

      <section style={{ display: 'flex', flexWrap: 'wrap', gap: 9, padding: 12, border: '1px solid #e6eaf0', borderRadius: 9, background: '#fff' }} aria-label="Lead filters">
        <input
          type="search"
          aria-label="Search by name, email or phone"
          placeholder="Search by name, email or phone"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          style={searchStyle}
        />
        <select aria-label="Filter by status" value={status} onChange={(event) => setStatus(event.target.value)} style={controlStyle}>
          <option value="">All statuses</option>
          {STATUSES.map((item) => <option key={item} value={item}>{humanize(item)}</option>)}
        </select>
        <select aria-label="Filter by source" value={source} onChange={(event) => setSource(event.target.value)} style={controlStyle}>
          <option value="">All sources</option>
          {SOURCES.map((item) => <option key={item} value={item}>{humanize(item)}</option>)}
        </select>
      </section>

      {loading ? (
        <section role="status" style={{ ...pageStyle, placeItems: 'center', padding: 48, border: '1px solid #e6eaf0', borderRadius: 9, background: '#fff' }}>
          Loading leads…
        </section>
      ) : error ? (
        <section style={{ display: 'grid', justifyItems: 'center', gap: 10, padding: 40, border: '1px solid #e6eaf0', borderRadius: 9, background: '#fff', color: '#65758a' }}>
          <p>Leads could not be loaded.</p>
          <button type="button" style={secondaryButtonStyle} onClick={loadLeads}>Try again</button>
        </section>
      ) : leads.length ? (
        <section style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 320px), 1fr))', gap: 13 }} aria-label="Your leads">
          {leads.map((lead) => (
            <article key={lead.id} style={{ padding: 15, border: '1px solid #e6eaf0', borderRadius: 9, background: '#fff' }}>
              <header style={{ display: 'flex', justifyContent: 'space-between', gap: 10, marginBottom: 12 }}>
                <div>
                  <h3 style={{ margin: 0, color: '#24354a', fontSize: 15 }}>{lead.name}</h3>
                  <p style={{ margin: '4px 0 0', color: '#718096', fontSize: 10 }}>
                    {humanize(lead.source)} · {humanize(lead.status)}
                  </p>
                </div>
                <span style={{ alignSelf: 'start', padding: '4px 8px', borderRadius: 99, background: '#edf3fa', color: '#355f91', fontSize: 9, fontWeight: 700 }}>
                  {humanize(lead.status)}
                </span>
              </header>
              <dl style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '9px 12px', margin: 0, fontSize: 10 }}>
                {[
                  ['Phone', lead.phone || 'Not provided'],
                  ['Email', lead.email || 'Not provided'],
                  ['Interested property', lead.interestedPropertyTitle || 'None selected'],
                  ['Budget', formatBudget(lead.budgetMin, lead.budgetMax)],
                  ['Preferred location', lead.preferredLocation || 'Not specified'],
                  ['Created', lead.createdAt ? new Date(lead.createdAt).toLocaleDateString() : '—'],
                ].map(([term, value]) => (
                  <div key={term} style={{ minWidth: 0 }}>
                    <dt style={{ color: '#8995a4' }}>{term}</dt>
                    <dd style={{ overflowWrap: 'anywhere', margin: '3px 0 0', color: '#46566b' }}>{value}</dd>
                  </div>
                ))}
              </dl>
              <footer style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 14, paddingTop: 10, borderTop: '1px solid #eef0f3' }}>
                <button type="button" style={{ ...secondaryButtonStyle, minHeight: 28, padding: '3px 8px' }} onClick={() => setModalLead(lead)}>View</button>
                <button type="button" style={{ ...secondaryButtonStyle, minHeight: 28, padding: '3px 8px' }} onClick={() => editLead(lead)}>Edit</button>
                <button type="button" style={{ ...secondaryButtonStyle, minHeight: 28, padding: '3px 8px', color: '#a34848' }} onClick={() => deleteLeadRecord(lead)}>Delete</button>
              </footer>
            </article>
          ))}
        </section>
      ) : search || status || source ? (
        <section style={{ padding: 40, border: '1px solid #e6eaf0', borderRadius: 9, background: '#fff', color: '#718096', textAlign: 'center', fontSize: 12 }}>
          No leads match the selected search and filters.
        </section>
      ) : (
        <section style={{ display: 'grid', justifyItems: 'center', gap: 9, padding: 40, border: '1px solid #e6eaf0', borderRadius: 9, background: '#fff', textAlign: 'center' }}>
          <span aria-hidden="true" style={{ color: '#426a94', fontSize: 26 }}>◎</span>
          <h3 style={{ margin: 0, color: '#34455b', fontSize: 15 }}>No leads yet</h3>
          <p style={{ margin: 0, color: '#818da0', fontSize: 11 }}>Add your first lead to start managing enquiries.</p>
          <button type="button" style={primaryButtonStyle} onClick={() => setModalLead(null)}>Add your first lead</button>
        </section>
      )}

      {modalLead !== undefined && (
        <LeadForm
          lead={modalLead}
          properties={properties}
          onCancel={() => setModalLead(undefined)}
          onSave={saveLead}
        />
      )}
    </div>
  );
}
