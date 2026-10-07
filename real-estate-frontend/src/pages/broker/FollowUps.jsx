import { useCallback, useEffect, useState } from 'react';
import {
  createFollowUp,
  deleteFollowUp,
  fetchCsrfToken,
  fetchFollowUp,
  fetchFollowUps,
  fetchLeads,
  updateFollowUp,
} from '../../api';

const STATUSES = ['PENDING', 'COMPLETED', 'CANCELLED'];
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'];
const EMPTY_FORM = {
  title: '',
  description: '',
  leadId: '',
  followUpAt: '',
  status: 'PENDING',
  priority: 'MEDIUM',
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
const panel = {
  padding: 16,
  border: '1px solid #e6eaf0',
  borderRadius: 9,
  background: '#fff',
};

function humanize(value) {
  return value
    ? value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (character) => character.toUpperCase())
    : '—';
}

function toLocalDateTime(value) {
  if (!value) return '';
  const date = new Date(value);
  date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
  return date.toISOString().slice(0, 16);
}

function dateFilterStart(date) {
  return date ? new Date(`${date}T00:00:00`).toISOString() : '';
}

function dateFilterEnd(date) {
  return date ? new Date(`${date}T23:59:59.999`).toISOString() : '';
}

function FollowUpForm({ followUp, leads, onCancel, onSave }) {
  const [form, setForm] = useState(followUp ? {
    title: followUp.title,
    description: followUp.description || '',
    leadId: String(followUp.leadId),
    followUpAt: toLocalDateTime(followUp.followUpAt),
    status: followUp.status,
    priority: followUp.priority,
  } : EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const labelStyle = { display: 'grid', gap: 5, color: '#526176', fontSize: 11 };

  function change(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    setError('');
    try {
      await onSave({
        title: form.title.trim(),
        description: form.description.trim() || null,
        leadId: Number(form.leadId),
        followUpAt: new Date(form.followUpAt).toISOString(),
        status: form.status,
        priority: form.priority,
      });
    } catch (saveError) {
      setError(saveError.message || 'Unable to save this follow-up.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onCancel();
      }}
      style={{
        position: 'fixed', zIndex: 60, inset: 0, display: 'grid', placeItems: 'center',
        overflowY: 'auto', padding: 16, background: 'rgba(22, 34, 51, .48)',
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="follow-up-form-title"
        style={{
          width: 'min(100%, 620px)', maxHeight: '90vh', overflowY: 'auto', padding: 20,
          border: '1px solid #e4e8ee', borderRadius: 10, background: '#fff',
          boxShadow: '0 18px 60px rgba(18, 30, 48, .2)',
        }}
      >
        <header style={{ marginBottom: 16 }}>
          <p style={{ margin: 0, color: '#8492a5', fontSize: 9, fontWeight: 750, letterSpacing: 1 }}>CUSTOMER RELATIONSHIP</p>
          <h2 id="follow-up-form-title" style={{ margin: '4px 0 0', color: '#1e3046', fontSize: 18 }}>
            {followUp ? 'Edit Follow-up' : 'Add Follow-up'}
          </h2>
        </header>
        {error && <div role="alert" style={{ marginBottom: 12, padding: 10, borderRadius: 7, background: '#fff5f5', color: '#a33636', fontSize: 11 }}>{error}</div>}
        {!leads.length && <p role="alert" style={{ color: '#a33636', fontSize: 11 }}>Add a lead before creating a follow-up.</p>}
        <form onSubmit={submit}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: 12 }}>
            <label style={labelStyle}>Title
              <input name="title" value={form.title} onChange={change} maxLength={160} required style={inputStyle} />
            </label>
            <label style={labelStyle}>Related lead
              <select name="leadId" value={form.leadId} onChange={change} required style={inputStyle}>
                <option value="">Select a lead</option>
                {leads.map((lead) => <option key={lead.id} value={lead.id}>{lead.name}</option>)}
              </select>
            </label>
            <label style={labelStyle}>Follow-up date and time
              <input type="datetime-local" name="followUpAt" value={form.followUpAt} onChange={change} required style={inputStyle} />
            </label>
            <label style={labelStyle}>Priority
              <select name="priority" value={form.priority} onChange={change} style={inputStyle}>
                {PRIORITIES.map((priority) => <option key={priority} value={priority}>{humanize(priority)}</option>)}
              </select>
            </label>
            <label style={labelStyle}>Status
              <select name="status" value={form.status} onChange={change} style={inputStyle}>
                {STATUSES.map((status) => <option key={status} value={status}>{humanize(status)}</option>)}
              </select>
            </label>
            <label style={{ ...labelStyle, gridColumn: '1 / -1' }}>Description / notes
              <textarea name="description" value={form.description} onChange={change} maxLength={3000} rows={4} style={{ ...inputStyle, resize: 'vertical' }} />
            </label>
          </div>
          <footer style={{ display: 'flex', justifyContent: 'flex-end', gap: 9, marginTop: 16 }}>
            <button type="button" style={secondaryButton} onClick={onCancel}>Cancel</button>
            <button type="submit" style={primaryButton} disabled={saving || !leads.length}>{saving ? 'Saving…' : 'Save Follow-up'}</button>
          </footer>
        </form>
      </section>
    </div>
  );
}

function FollowUpDetails({ followUp, onClose }) {
  return (
    <div
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
      style={{ position: 'fixed', zIndex: 60, inset: 0, display: 'grid', placeItems: 'center', padding: 16, background: 'rgba(22, 34, 51, .48)' }}
    >
      <section role="dialog" aria-modal="true" aria-labelledby="follow-up-detail-title" style={{ ...panel, width: 'min(100%, 480px)', boxShadow: '0 18px 60px rgba(18, 30, 48, .2)' }}>
        <h2 id="follow-up-detail-title" style={{ margin: '0 0 14px', color: '#1e3046', fontSize: 18 }}>{followUp.title}</h2>
        <dl style={{ display: 'grid', gap: 11, margin: 0, fontSize: 11 }}>
          {[
            ['Lead', followUp.leadName],
            ['Date and time', followUp.followUpAt ? new Date(followUp.followUpAt).toLocaleString() : '—'],
            ['Priority', humanize(followUp.priority)],
            ['Status', humanize(followUp.status)],
            ['Notes', followUp.description || '—'],
          ].map(([term, value]) => (
            <div key={term}><dt style={{ color: '#8995a4' }}>{term}</dt><dd style={{ margin: '3px 0 0', color: '#46566b', whiteSpace: 'pre-wrap' }}>{value}</dd></div>
          ))}
        </dl>
        <footer style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16 }}>
          <button type="button" style={secondaryButton} onClick={onClose}>Close</button>
        </footer>
      </section>
    </div>
  );
}

export default function FollowUps({ csrfToken }) {
  const [followUps, setFollowUps] = useState([]);
  const [leads, setLeads] = useState([]);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [priority, setPriority] = useState('');
  const [leadId, setLeadId] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [formFollowUp, setFormFollowUp] = useState(undefined);
  const [viewFollowUp, setViewFollowUp] = useState(null);

  const loadFollowUps = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const filters = {
        search,
        status,
        priority,
        leadId,
        from: dateFilterStart(fromDate),
        to: dateFilterEnd(toDate),
      };
      setFollowUps(await fetchFollowUps(filters));
    } catch (requestError) {
      setError(requestError.message || 'Unable to load follow-ups.');
    } finally {
      setLoading(false);
    }
  }, [search, status, priority, leadId, fromDate, toDate]);

  useEffect(() => {
    let cancelled = false;
    fetchLeads()
      .then((data) => {
        if (!cancelled) setLeads(data);
      })
      .catch((requestError) => {
        if (!cancelled) setError(requestError.message || 'Unable to load your leads.');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    fetchFollowUps({
      search,
      status,
      priority,
      leadId,
      from: dateFilterStart(fromDate),
      to: dateFilterEnd(toDate),
    })
      .then((data) => {
        if (!cancelled) {
          setFollowUps(data);
          setError('');
        }
      })
      .catch((requestError) => {
        if (!cancelled) setError(requestError.message || 'Unable to load follow-ups.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [search, status, priority, leadId, fromDate, toDate]);

  async function writeToken() {
    if (csrfToken) return csrfToken;
    return (await fetchCsrfToken()).token;
  }

  async function saveFollowUp(payload) {
    const token = await writeToken();
    if (formFollowUp) {
      await updateFollowUp(formFollowUp.id, payload, token);
      setSuccess('Follow-up updated successfully.');
    } else {
      await createFollowUp(payload, token);
      setSuccess('Follow-up added successfully.');
    }
    setFormFollowUp(undefined);
    await loadFollowUps();
  }

  async function openDetails(followUp) {
    setError('');
    try {
      setViewFollowUp(await fetchFollowUp(followUp.id));
    } catch (requestError) {
      setError(requestError.message || 'Unable to load follow-up details.');
    }
  }

  async function editFollowUp(followUp) {
    setError('');
    try {
      setFormFollowUp(await fetchFollowUp(followUp.id));
    } catch (requestError) {
      setError(requestError.message || 'Unable to load follow-up details.');
    }
  }

  async function removeFollowUp(followUp) {
    if (!window.confirm(`Delete the follow-up "${followUp.title}"?`)) return;
    setError('');
    setSuccess('');
    try {
      await deleteFollowUp(followUp.id, await writeToken());
      setSuccess('Follow-up deleted.');
      await loadFollowUps();
    } catch (requestError) {
      setError(requestError.message || 'Unable to delete this follow-up.');
    }
  }

  async function markCompleted(followUp) {
    setError('');
    setSuccess('');
    try {
      const details = await fetchFollowUp(followUp.id);
      await updateFollowUp(followUp.id, {
        title: details.title,
        description: details.description,
        leadId: details.leadId,
        followUpAt: details.followUpAt,
        status: 'COMPLETED',
        priority: details.priority,
      }, await writeToken());
      setSuccess('Follow-up marked as completed.');
      await loadFollowUps();
    } catch (requestError) {
      setError(requestError.message || 'Unable to complete this follow-up.');
    }
  }

  return (
    <div style={{ display: 'grid', gap: 16, color: '#172033' }}>
      <header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 14, flexWrap: 'wrap' }}>
        <div>
          <p style={{ margin: '0 0 3px', color: '#8492a5', fontSize: 9, fontWeight: 750, letterSpacing: 1 }}>CUSTOMER RELATIONSHIPS</p>
          <h2 style={{ margin: 0, color: '#1c2c40', fontSize: 23 }}>Follow-ups</h2>
          <p style={{ margin: '4px 0 0', color: '#718096', fontSize: 12 }}>Keep track of your next steps with leads</p>
        </div>
        <button type="button" style={primaryButton} onClick={() => setFormFollowUp(null)}>＋ Add Follow-up</button>
      </header>

      {success && <div role="status" style={{ padding: 10, borderRadius: 7, background: '#f1faf4', color: '#27643d', fontSize: 11 }}>{success}</div>}
      {error && <div role="alert" style={{ padding: 10, borderRadius: 7, background: '#fff5f5', color: '#a33636', fontSize: 11 }}>{error}</div>}

      <section style={{ display: 'flex', flexWrap: 'wrap', gap: 9, padding: 12, border: '1px solid #e6eaf0', borderRadius: 9, background: '#fff' }} aria-label="Follow-up filters">
        <input type="search" aria-label="Search follow-ups" placeholder="Search title, notes, or lead" value={search} onChange={(event) => setSearch(event.target.value)} style={{ ...inputStyle, minWidth: 190, flex: '1 1 230px' }} />
        <select aria-label="Filter by lead" value={leadId} onChange={(event) => setLeadId(event.target.value)} style={{ ...inputStyle, width: 'auto', minWidth: 150 }}>
          <option value="">All leads</option>
          {leads.map((lead) => <option key={lead.id} value={lead.id}>{lead.name}</option>)}
        </select>
        <select aria-label="Filter by status" value={status} onChange={(event) => setStatus(event.target.value)} style={{ ...inputStyle, width: 'auto', minWidth: 140 }}>
          <option value="">All statuses</option>
          {STATUSES.map((item) => <option key={item} value={item}>{humanize(item)}</option>)}
        </select>
        <select aria-label="Filter by priority" value={priority} onChange={(event) => setPriority(event.target.value)} style={{ ...inputStyle, width: 'auto', minWidth: 130 }}>
          <option value="">All priorities</option>
          {PRIORITIES.map((item) => <option key={item} value={item}>{humanize(item)}</option>)}
        </select>
        <label style={{ display: 'grid', gap: 3, color: '#718096', fontSize: 9 }}>From
          <input aria-label="Filter from date" type="date" value={fromDate} onChange={(event) => setFromDate(event.target.value)} style={inputStyle} />
        </label>
        <label style={{ display: 'grid', gap: 3, color: '#718096', fontSize: 9 }}>To
          <input aria-label="Filter to date" type="date" value={toDate} onChange={(event) => setToDate(event.target.value)} style={inputStyle} />
        </label>
      </section>

      {loading ? (
        <section role="status" style={{ ...panel, padding: 42, color: '#718096', textAlign: 'center', fontSize: 12 }}>Loading follow-ups…</section>
      ) : error ? (
        <section style={{ ...panel, display: 'grid', justifyItems: 'center', gap: 10, padding: 40, color: '#718096', fontSize: 12 }}>
          Follow-ups could not be loaded.
          <button type="button" style={secondaryButton} onClick={loadFollowUps}>Try again</button>
        </section>
      ) : followUps.length ? (
        <section style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 320px), 1fr))', gap: 13 }} aria-label="Follow-ups">
          {followUps.map((followUp) => (
            <article key={followUp.id} style={panel}>
              <header style={{ display: 'flex', justifyContent: 'space-between', gap: 10 }}>
                <div>
                  <h3 style={{ margin: 0, color: '#24354a', fontSize: 14 }}>{followUp.title}</h3>
                  <p style={{ margin: '4px 0 0', color: '#66758a', fontSize: 11 }}>Lead: {followUp.leadName}</p>
                </div>
                <span style={{ alignSelf: 'start', padding: '5px 8px', borderRadius: 99, background: followUp.priority === 'HIGH' ? '#fff1ed' : '#edf3fa', color: followUp.priority === 'HIGH' ? '#a34c35' : '#355f91', fontSize: 9, fontWeight: 700 }}>
                  {humanize(followUp.priority)}
                </span>
              </header>
              <p style={{ margin: '12px 0 4px', color: '#46566b', fontSize: 11 }}>
                {followUp.followUpAt ? new Date(followUp.followUpAt).toLocaleString() : '—'}
              </p>
              <span style={{ display: 'inline-block', marginTop: 3, padding: '4px 8px', borderRadius: 99, background: '#f4f6f8', color: '#607086', fontSize: 9, fontWeight: 700 }}>
                {humanize(followUp.status)}
              </span>
              {followUp.description && <p style={{ margin: '10px 0 0', color: '#637287', fontSize: 10, lineHeight: 1.5 }}>{followUp.description}</p>}
              <footer style={{ display: 'flex', justifyContent: 'flex-end', flexWrap: 'wrap', gap: 8, marginTop: 13, paddingTop: 10, borderTop: '1px solid #eef0f3' }}>
                <button type="button" style={{ ...secondaryButton, minHeight: 28, padding: '3px 8px' }} onClick={() => openDetails(followUp)}>View</button>
                <button type="button" style={{ ...secondaryButton, minHeight: 28, padding: '3px 8px' }} onClick={() => editFollowUp(followUp)}>Edit</button>
                {followUp.status === 'PENDING' && <button type="button" style={{ ...secondaryButton, minHeight: 28, padding: '3px 8px', color: '#27643d' }} onClick={() => markCompleted(followUp)}>Mark completed</button>}
                <button type="button" style={{ ...secondaryButton, minHeight: 28, padding: '3px 8px', color: '#a34848' }} onClick={() => removeFollowUp(followUp)}>Delete</button>
              </footer>
            </article>
          ))}
        </section>
      ) : (
        <section style={{ ...panel, display: 'grid', justifyItems: 'center', gap: 9, padding: 40, textAlign: 'center' }}>
          <span aria-hidden="true" style={{ color: '#426a94', fontSize: 26 }}>↗</span>
          <h3 style={{ margin: 0, color: '#34455b', fontSize: 15 }}>{search || status || priority || leadId || fromDate || toDate ? 'No follow-ups match your filters' : 'No follow-ups yet'}</h3>
          <p style={{ margin: 0, color: '#818da0', fontSize: 11 }}>Create a follow-up to plan your next contact with a lead.</p>
          <button type="button" style={primaryButton} onClick={() => setFormFollowUp(null)}>Add Follow-up</button>
        </section>
      )}

      {formFollowUp !== undefined && (
        <FollowUpForm
          followUp={formFollowUp}
          leads={leads}
          onCancel={() => setFormFollowUp(undefined)}
          onSave={saveFollowUp}
        />
      )}
      {viewFollowUp && <FollowUpDetails followUp={viewFollowUp} onClose={() => setViewFollowUp(null)} />}
    </div>
  );
}
