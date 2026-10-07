import { useCallback, useEffect, useState } from 'react';
import {
  createSiteVisit,
  deleteSiteVisit,
  fetchCsrfToken,
  fetchLeads,
  fetchProperties,
  fetchSiteVisit,
  fetchSiteVisits,
  updateSiteVisit,
} from '../../api';

const STATUSES = ['SCHEDULED', 'COMPLETED', 'CANCELLED', 'RESCHEDULED'];
const EMPTY_FORM = {
  leadId: '',
  propertyId: '',
  scheduledAt: '',
  status: 'SCHEDULED',
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
const buttonStyle = {
  ...inputStyle,
  width: 'auto',
  borderColor: '#2459a6',
  background: '#2459a6',
  color: '#fff',
  fontWeight: 650,
  cursor: 'pointer',
};
const secondaryButtonStyle = {
  ...inputStyle,
  width: 'auto',
  background: '#fff',
  color: '#315f93',
  fontWeight: 600,
  cursor: 'pointer',
};

function humanize(value) {
  return value
    ? value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (character) => character.toUpperCase())
    : '—';
}

function toLocalDateTime(iso) {
  if (!iso) return '';
  const date = new Date(iso);
  date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
  return date.toISOString().slice(0, 16);
}

function SiteVisitForm({ visit, leads, properties, onCancel, onSave }) {
  const [form, setForm] = useState(visit ? {
    leadId: String(visit.leadId),
    propertyId: String(visit.propertyId),
    scheduledAt: toLocalDateTime(visit.scheduledAt),
    status: visit.status,
    notes: visit.notes || '',
  } : EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    setFormError('');
    try {
      await onSave({
        leadId: Number(form.leadId),
        propertyId: Number(form.propertyId),
        scheduledAt: new Date(form.scheduledAt).toISOString(),
        status: form.status,
        notes: form.notes.trim() || null,
      });
    } catch (error) {
      setFormError(error.message || 'Unable to save this site visit.');
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
        aria-labelledby="site-visit-form-title"
        style={{
          width: 'min(100%, 620px)',
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
          <p style={{ margin: 0, color: '#8492a5', fontSize: 9, fontWeight: 750, letterSpacing: 1 }}>APPOINTMENT</p>
          <h2 id="site-visit-form-title" style={{ margin: '4px 0 0', color: '#1e3046', fontSize: 18 }}>
            {visit ? 'Edit Site Visit' : 'Add Site Visit'}
          </h2>
        </header>
        <form onSubmit={submit}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: 12 }}>
            <label style={labelStyle}>Lead
              <select style={inputStyle} name="leadId" value={form.leadId} onChange={updateField} required>
                <option value="">Select a lead</option>
                {leads.map((lead) => <option key={lead.id} value={lead.id}>{lead.name}</option>)}
              </select>
            </label>
            <label style={labelStyle}>Property
              <select style={inputStyle} name="propertyId" value={form.propertyId} onChange={updateField} required>
                <option value="">Select a property</option>
                {properties.map((property) => (
                  <option key={property.id} value={property.id}>{property.title} · {property.city}</option>
                ))}
              </select>
            </label>
            <label style={labelStyle}>Scheduled date and time
              <input
                style={inputStyle}
                type="datetime-local"
                name="scheduledAt"
                value={form.scheduledAt}
                onChange={updateField}
                required
              />
            </label>
            <label style={labelStyle}>Status
              <select style={inputStyle} name="status" value={form.status} onChange={updateField} required>
                {STATUSES.map((status) => <option key={status} value={status}>{humanize(status)}</option>)}
              </select>
            </label>
            <label style={{ ...labelStyle, gridColumn: '1 / -1' }}>Notes
              <textarea
                style={{ ...inputStyle, resize: 'vertical' }}
                name="notes"
                value={form.notes}
                onChange={updateField}
                rows={4}
                maxLength={3000}
              />
            </label>
          </div>
          {formError && <p role="alert" style={{ color: '#a33636', fontSize: 11 }}>{formError}</p>}
          <footer style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 16 }}>
            <button type="button" style={secondaryButtonStyle} onClick={onCancel}>Cancel</button>
            <button type="submit" style={buttonStyle} disabled={saving || !leads.length || !properties.length}>
              {saving ? 'Saving…' : visit ? 'Save Changes' : 'Add Site Visit'}
            </button>
          </footer>
        </form>
      </section>
    </div>
  );
}

export default function SiteVisits({ csrfToken }) {
  const [visits, setVisits] = useState([]);
  const [leads, setLeads] = useState([]);
  const [properties, setProperties] = useState([]);
  const [status, setStatus] = useState('');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [optionsError, setOptionsError] = useState('');
  const [success, setSuccess] = useState('');
  const [formVisit, setFormVisit] = useState(undefined);

  const loadVisits = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setVisits(await fetchSiteVisits(status));
    } catch (requestError) {
      setError(requestError.message || 'Unable to load site visits.');
    } finally {
      setLoading(false);
    }
  }, [status]);

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
    fetchSiteVisits(status)
      .then((data) => {
        if (!cancelled) setVisits(data);
      })
      .catch((requestError) => {
        if (!cancelled) setError(requestError.message || 'Unable to load site visits.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [status]);

  const visibleVisits = visits.filter((visit) => {
    const value = search.trim().toLowerCase();
    return !value || [visit.leadName, visit.propertyTitle, visit.propertyLocation]
      .some((field) => field?.toLowerCase().includes(value));
  });

  async function getWriteToken() {
    if (csrfToken) return csrfToken;
    const tokenResponse = await fetchCsrfToken();
    return tokenResponse.token;
  }

  async function saveVisit(payload) {
    const token = await getWriteToken();
    if (formVisit) {
      await updateSiteVisit(formVisit.id, payload, token);
      setSuccess('Site visit updated successfully.');
    } else {
      await createSiteVisit(payload, token);
      setSuccess('Site visit scheduled successfully.');
    }
    setFormVisit(undefined);
    await loadVisits();
  }

  async function editVisit(visit) {
    setError('');
    try {
      setFormVisit(await fetchSiteVisit(visit.id));
    } catch (requestError) {
      setError(requestError.message || 'Unable to load site visit details.');
    }
  }

  async function removeVisit(visit) {
    if (!window.confirm(`Delete the site visit for ${visit.leadName}?`)) return;
    setError('');
    setSuccess('');
    try {
      await deleteSiteVisit(visit.id, await getWriteToken());
      setSuccess('Site visit deleted.');
      await loadVisits();
    } catch (requestError) {
      setError(requestError.message || 'Unable to delete this site visit.');
    }
  }

  return (
    <div style={{ display: 'grid', gap: 16, color: '#172033' }}>
      <header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 14, flexWrap: 'wrap' }}>
        <div>
          <p style={{ margin: '0 0 3px', color: '#8492a5', fontSize: 9, fontWeight: 750, letterSpacing: 1 }}>APPOINTMENTS</p>
          <h2 style={{ margin: 0, color: '#1c2c40', fontSize: 23 }}>Site Visits</h2>
          <p style={{ margin: '4px 0 0', color: '#718096', fontSize: 12 }}>Manage property visits with your leads</p>
        </div>
        <button type="button" style={buttonStyle} onClick={() => setFormVisit(null)}>＋ Add Site Visit</button>
      </header>

      {success && <div role="status" style={{ padding: 10, borderRadius: 7, background: '#f1faf4', color: '#27643d', fontSize: 11 }}>{success}</div>}
      {error && <div role="alert" style={{ padding: 10, borderRadius: 7, background: '#fff5f5', color: '#a33636', fontSize: 11 }}>{error}</div>}
      {optionsError && <div role="alert" style={{ padding: 10, borderRadius: 7, background: '#fff5f5', color: '#a33636', fontSize: 11 }}>{optionsError}</div>}

      <section style={{ display: 'flex', flexWrap: 'wrap', gap: 9, padding: 12, border: '1px solid #e6eaf0', borderRadius: 9, background: '#fff' }} aria-label="Site visit filters">
        <input
          type="search"
          aria-label="Search by lead or property"
          placeholder="Search by lead or property"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          style={{ ...inputStyle, minWidth: 190, flex: '1 1 230px' }}
        />
        <select aria-label="Filter by status" value={status} onChange={(event) => setStatus(event.target.value)} style={{ ...inputStyle, width: 'auto', minWidth: 160 }}>
          <option value="">All statuses</option>
          {STATUSES.map((item) => <option key={item} value={item}>{humanize(item)}</option>)}
        </select>
      </section>

      {loading ? (
        <section role="status" style={{ ...cardStyle, padding: 42, color: '#718096', textAlign: 'center', fontSize: 12 }}>Loading site visits…</section>
      ) : error ? (
        <section style={{ ...cardStyle, display: 'grid', justifyItems: 'center', gap: 10, padding: 40, color: '#718096', fontSize: 12 }}>
          Site visits could not be loaded.
          <button type="button" style={secondaryButtonStyle} onClick={loadVisits}>Try again</button>
        </section>
      ) : visibleVisits.length ? (
        <section style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 320px), 1fr))', gap: 13 }} aria-label="Site visits">
          {visibleVisits.map((visit) => (
            <article key={visit.id} style={cardStyle}>
              <header style={{ display: 'flex', justifyContent: 'space-between', gap: 10 }}>
                <div>
                  <h3 style={{ margin: 0, color: '#24354a', fontSize: 14 }}>{visit.leadName}</h3>
                  <p style={{ margin: '4px 0 0', color: '#66758a', fontSize: 11 }}>{visit.propertyTitle}</p>
                </div>
                <span style={{ alignSelf: 'start', padding: '5px 8px', borderRadius: 99, background: '#edf3fa', color: '#355f91', fontSize: 9, fontWeight: 700 }}>
                  {humanize(visit.status)}
                </span>
              </header>
              <p style={{ margin: '12px 0 5px', color: '#46566b', fontSize: 11 }}>
                {new Date(visit.scheduledAt).toLocaleString()}
              </p>
              {visit.propertyLocation && <p style={{ margin: 0, color: '#718096', fontSize: 10 }}>{visit.propertyLocation}</p>}
              {visit.notes && <p style={{ margin: '10px 0 0', color: '#637287', fontSize: 10, lineHeight: 1.5 }}>{visit.notes}</p>}
              <footer style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 13, paddingTop: 10, borderTop: '1px solid #eef0f3' }}>
                <button type="button" style={{ ...secondaryButtonStyle, minHeight: 28, padding: '3px 8px' }} onClick={() => editVisit(visit)}>View / Edit</button>
                <button type="button" style={{ ...secondaryButtonStyle, minHeight: 28, padding: '3px 8px', color: '#a34848' }} onClick={() => removeVisit(visit)}>Delete</button>
              </footer>
            </article>
          ))}
        </section>
      ) : visits.length && search ? (
        <section style={{ ...cardStyle, padding: 40, color: '#718096', textAlign: 'center', fontSize: 12 }}>No visits match your search.</section>
      ) : (
        <section style={{ ...cardStyle, display: 'grid', justifyItems: 'center', gap: 9, padding: 40, textAlign: 'center' }}>
          <span aria-hidden="true" style={{ color: '#426a94', fontSize: 26 }}>▣</span>
          <h3 style={{ margin: 0, color: '#34455b', fontSize: 15 }}>No site visits scheduled</h3>
          <p style={{ margin: 0, color: '#818da0', fontSize: 11 }}>Schedule a visit with a lead to see it here.</p>
          <button type="button" style={buttonStyle} onClick={() => setFormVisit(null)}>Add Site Visit</button>
        </section>
      )}

      {formVisit !== undefined && (
        <SiteVisitForm
          visit={formVisit}
          leads={leads}
          properties={properties}
          onCancel={() => setFormVisit(undefined)}
          onSave={saveVisit}
        />
      )}
    </div>
  );
}

const cardStyle = {
  padding: 16,
  border: '1px solid #e6eaf0',
  borderRadius: 9,
  background: '#fff',
};
