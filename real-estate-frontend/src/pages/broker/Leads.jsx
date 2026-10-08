import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  createLead,
  deleteLead,
  fetchCsrfToken,
  fetchLeads,
  fetchProperties,
  updateLead,
} from '../../api';

const STATUSES = [
  'NEW',
  'CONTACTED',
  'QUALIFIED',
  'SITE_VISIT',
  'NEGOTIATION',
  'WON',
  'LOST',
];

const SOURCES = [
  'WEBSITE',
  'PHONE',
  'REFERRAL',
  'SOCIAL_MEDIA',
  'WALK_IN',
  'OTHER',
];

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

function humanize(value) {
  if (!value) return '—';

  return value
    .replaceAll('_', ' ')
    .toLowerCase()
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

function formatBudget(min, max) {
  const amount = (value) =>
    new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(value);

  if (min != null && max != null) {
    return `${amount(min)} – ${amount(max)}`;
  }

  if (min != null) return `From ${amount(min)}`;
  if (max != null) return `Up to ${amount(max)}`;

  return 'Not specified';
}

function statusColor(status) {
  const colors = {
    NEW: '#2563eb',
    CONTACTED: '#7c3aed',
    QUALIFIED: '#0891b2',
    SITE_VISIT: '#d97706',
    NEGOTIATION: '#ea580c',
    WON: '#16a34a',
    LOST: '#dc2626',
  };

  return colors[status] || '#64748b';
}

function LeadForm({ lead, properties, onCancel, onSave }) {
  const editing = Boolean(lead);

  const [form, setForm] = useState(
    lead
      ? {
          ...EMPTY_FORM,
          ...lead,
          interestedPropertyId: lead.interestedPropertyId ?? '',
          budgetMin: lead.budgetMin ?? '',
          budgetMax: lead.budgetMax ?? '',
        }
      : EMPTY_FORM,
  );

  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');

  function change(event) {
    setForm((current) => ({
      ...current,
      [event.target.name]: event.target.value,
    }));
  }

  async function submit(event) {
    event.preventDefault();

    setSaving(true);
    setFormError('');

    const payload = {
      ...form,
      name: form.name.trim(),
      email: form.email.trim() || null,
      phone: form.phone.trim() || null,
      interestedPropertyId: form.interestedPropertyId
        ? Number(form.interestedPropertyId)
        : null,
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

  const inputStyle = {
    width: '100%',
    boxSizing: 'border-box',
    minHeight: 42,
    padding: '10px 12px',
    border: '1px solid #dbe3ee',
    borderRadius: 10,
    background: '#fff',
    color: '#172033',
    fontSize: 13,
    outline: 'none',
  };

  const labelStyle = {
    display: 'grid',
    gap: 7,
    color: '#526176',
    fontSize: 12,
    fontWeight: 600,
  };

  return (
    <div
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onCancel();
      }}
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 100,
        display: 'grid',
        placeItems: 'center',
        padding: 20,
        background: 'rgba(15, 23, 42, 0.55)',
        backdropFilter: 'blur(5px)',
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        style={{
          width: 'min(720px, 100%)',
          maxHeight: '90vh',
          overflowY: 'auto',
          background: '#fff',
          borderRadius: 18,
          boxShadow: '0 25px 80px rgba(15, 23, 42, .25)',
          padding: 26,
        }}
      >
        <header
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            marginBottom: 22,
          }}
        >
          <div>
            <div
              style={{
                color: '#315f93',
                fontSize: 10,
                fontWeight: 800,
                letterSpacing: 1.2,
                marginBottom: 5,
              }}
            >
              CUSTOMER RELATIONSHIP
            </div>

            <h2
              style={{
                margin: 0,
                fontSize: 24,
                color: '#16243a',
              }}
            >
              {editing ? 'Edit Lead' : 'Add New Lead'}
            </h2>

            <p
              style={{
                margin: '6px 0 0',
                color: '#718096',
                fontSize: 13,
              }}
            >
              Capture customer requirements and follow-up information.
            </p>
          </div>

          <button
            type="button"
            onClick={onCancel}
            style={{
              border: '1px solid #dbe3ee',
              background: '#f8fafc',
              borderRadius: 9,
              width: 36,
              height: 36,
              cursor: 'pointer',
              fontSize: 20,
              color: '#475569',
            }}
          >
            ×
          </button>
        </header>

        <form onSubmit={submit}>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
              gap: 15,
            }}
          >
            <label style={labelStyle}>
              Full name *
              <input
                style={inputStyle}
                name="name"
                value={form.name}
                onChange={change}
                required
                maxLength={120}
                placeholder="Enter customer name"
              />
            </label>

            <label style={labelStyle}>
              Email
              <input
                style={inputStyle}
                type="email"
                name="email"
                value={form.email || ''}
                onChange={change}
                placeholder="customer@email.com"
              />
            </label>

            <label style={labelStyle}>
              Phone
              <input
                style={inputStyle}
                type="tel"
                name="phone"
                value={form.phone || ''}
                onChange={change}
                placeholder="+91"
              />
            </label>

            <label style={labelStyle}>
              Lead source
              <select
                style={inputStyle}
                name="source"
                value={form.source}
                onChange={change}
              >
                {SOURCES.map((source) => (
                  <option key={source} value={source}>
                    {humanize(source)}
                  </option>
                ))}
              </select>
            </label>

            <label style={labelStyle}>
              Status
              <select
                style={inputStyle}
                name="status"
                value={form.status}
                onChange={change}
              >
                {STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {humanize(status)}
                  </option>
                ))}
              </select>
            </label>

            <label style={labelStyle}>
              Interested property
              <select
                style={inputStyle}
                name="interestedPropertyId"
                value={form.interestedPropertyId}
                onChange={change}
              >
                <option value="">No property selected</option>

                {properties.map((property) => (
                  <option key={property.id} value={property.id}>
                    {property.title} · {property.city}
                  </option>
                ))}
              </select>
            </label>

            <label style={labelStyle}>
              Minimum budget
              <input
                style={inputStyle}
                type="number"
                min="0"
                name="budgetMin"
                value={form.budgetMin}
                onChange={change}
                placeholder="₹ Minimum"
              />
            </label>

            <label style={labelStyle}>
              Maximum budget
              <input
                style={inputStyle}
                type="number"
                min="0"
                name="budgetMax"
                value={form.budgetMax}
                onChange={change}
                placeholder="₹ Maximum"
              />
            </label>

            <label style={labelStyle}>
              Preferred location
              <input
                style={inputStyle}
                name="preferredLocation"
                value={form.preferredLocation || ''}
                onChange={change}
                placeholder="Bangalore, Whitefield..."
              />
            </label>

            <label
              style={{
                ...labelStyle,
                gridColumn: '1 / -1',
              }}
            >
              Notes
              <textarea
                style={{
                  ...inputStyle,
                  minHeight: 100,
                  resize: 'vertical',
                }}
                name="notes"
                value={form.notes || ''}
                onChange={change}
                placeholder="Add requirements, preferences or follow-up notes..."
              />
            </label>
          </div>

          {formError && (
            <div
              style={{
                marginTop: 15,
                padding: 11,
                borderRadius: 9,
                background: '#fff1f2',
                color: '#be123c',
                fontSize: 12,
              }}
            >
              {formError}
            </div>
          )}

          <footer
            style={{
              display: 'flex',
              justifyContent: 'flex-end',
              gap: 10,
              marginTop: 22,
              paddingTop: 18,
              borderTop: '1px solid #edf1f6',
            }}
          >
            <button
              type="button"
              onClick={onCancel}
              style={{
                padding: '10px 17px',
                borderRadius: 9,
                border: '1px solid #dbe3ee',
                background: '#fff',
                color: '#475569',
                cursor: 'pointer',
                fontWeight: 600,
              }}
            >
              Cancel
            </button>

            <button
              type="submit"
              disabled={saving}
              style={{
                padding: '10px 18px',
                borderRadius: 9,
                border: 0,
                background: 'linear-gradient(135deg, #2459a6, #3f6fc0)',
                color: '#fff',
                cursor: saving ? 'not-allowed' : 'pointer',
                fontWeight: 700,
                opacity: saving ? 0.7 : 1,
              }}
            >
              {saving ? 'Saving...' : editing ? 'Save Changes' : 'Create Lead'}
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
      const data = await fetchLeads({
        search,
        status,
        source,
      });

      setLeads(Array.isArray(data) ? data : []);
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
        if (!cancelled) {
          setProperties(Array.isArray(data) ? data : []);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setProperties([]);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    loadLeads();
  }, [loadLeads]);

  useEffect(() => {
    if (!success) return;

    const timer = setTimeout(() => setSuccess(''), 3500);

    return () => clearTimeout(timer);
  }, [success]);

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
      setSuccess('Lead created successfully.');
    }

    setModalLead(undefined);
    await loadLeads();
  }

  async function deleteLeadRecord(lead) {
    const confirmed = window.confirm(
      `Delete the lead for "${lead.name}"? This cannot be undone.`,
    );

    if (!confirmed) return;

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

  const summary = useMemo(() => {
    return {
      total: leads.length,
      new: leads.filter((lead) => lead.status === 'NEW').length,
      active: leads.filter(
        (lead) =>
          !['WON', 'LOST'].includes(lead.status),
      ).length,
      won: leads.filter((lead) => lead.status === 'WON').length,
    };
  }, [leads]);

  const inputStyle = {
    height: 42,
    padding: '0 12px',
    border: '1px solid #dbe3ee',
    borderRadius: 10,
    background: '#fff',
    color: '#26374c',
    fontSize: 13,
    outline: 'none',
  };

  const actionButton = {
    border: '1px solid #dbe3ee',
    background: '#fff',
    color: '#315f93',
    borderRadius: 8,
    padding: '7px 11px',
    cursor: 'pointer',
    fontSize: 11,
    fontWeight: 700,
  };

  return (
    <div
      style={{
        display: 'grid',
        gap: 18,
        color: '#172033',
        maxWidth: 1400,
      }}
    >
      {/* HEADER */}
      <header
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: 16,
          flexWrap: 'wrap',
        }}
      >
        <div>
          <div
            style={{
              color: '#718096',
              fontSize: 10,
              fontWeight: 800,
              letterSpacing: 1.3,
              marginBottom: 5,
            }}
          >
            CUSTOMER RELATIONSHIPS
          </div>

          <h1
            style={{
              margin: 0,
              fontSize: 28,
              color: '#18283e',
            }}
          >
            Leads
          </h1>

          <p
            style={{
              margin: '6px 0 0',
              color: '#718096',
              fontSize: 13,
            }}
          >
            Manage enquiries, prospects and customer relationships.
          </p>
        </div>

        <button
          type="button"
          onClick={() => setModalLead(null)}
          style={{
            border: 0,
            borderRadius: 10,
            padding: '11px 17px',
            background: 'linear-gradient(135deg, #2459a6, #3f6fc0)',
            color: '#fff',
            fontWeight: 700,
            cursor: 'pointer',
            boxShadow: '0 8px 20px rgba(37, 89, 166, .18)',
          }}
        >
          ＋ Add Lead
        </button>
      </header>

      {/* NOTIFICATIONS */}
      {success && (
        <div
          style={{
            padding: 12,
            borderRadius: 10,
            background: '#ecfdf3',
            border: '1px solid #bbf7d0',
            color: '#166534',
            fontSize: 12,
            fontWeight: 600,
          }}
        >
          ✓ {success}
        </div>
      )}

      {error && (
        <div
          style={{
            padding: 12,
            borderRadius: 10,
            background: '#fff1f2',
            border: '1px solid #fecdd3',
            color: '#be123c',
            fontSize: 12,
          }}
        >
          {error}
        </div>
      )}

      {/* SUMMARY */}
      <section
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
          gap: 13,
        }}
      >
        {[
          ['Total Leads', summary.total, 'All enquiries'],
          ['New Leads', summary.new, 'Need attention'],
          ['Active Leads', summary.active, 'Currently in progress'],
          ['Won Leads', summary.won, 'Converted customers'],
        ].map(([title, value, description]) => (
          <article
            key={title}
            style={{
              background: '#fff',
              border: '1px solid #e4eaf2',
              borderRadius: 14,
              padding: 18,
              boxShadow: '0 5px 18px rgba(31, 52, 80, .04)',
            }}
          >
            <div
              style={{
                color: '#718096',
                fontSize: 11,
                fontWeight: 700,
              }}
            >
              {title}
            </div>

            <div
              style={{
                marginTop: 8,
                fontSize: 27,
                fontWeight: 800,
                color: '#1d3554',
              }}
            >
              {value}
            </div>

            <div
              style={{
                marginTop: 4,
                color: '#9aa6b5',
                fontSize: 11,
              }}
            >
              {description}
            </div>
          </article>
        ))}
      </section>

      {/* FILTERS */}
      <section
        style={{
          padding: 15,
          borderRadius: 14,
          background: '#fff',
          border: '1px solid #e4eaf2',
          display: 'flex',
          flexWrap: 'wrap',
          gap: 10,
          alignItems: 'center',
        }}
      >
        <input
          type="search"
          placeholder="Search name, email or phone..."
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          style={{
            ...inputStyle,
            flex: '1 1 280px',
          }}
        />

        <select
          value={status}
          onChange={(event) => setStatus(event.target.value)}
          style={inputStyle}
        >
          <option value="">All statuses</option>

          {STATUSES.map((item) => (
            <option key={item} value={item}>
              {humanize(item)}
            </option>
          ))}
        </select>

        <select
          value={source}
          onChange={(event) => setSource(event.target.value)}
          style={inputStyle}
        >
          <option value="">All sources</option>

          {SOURCES.map((item) => (
            <option key={item} value={item}>
              {humanize(item)}
            </option>
          ))}
        </select>

        {(search || status || source) && (
          <button
            type="button"
            onClick={() => {
              setSearch('');
              setStatus('');
              setSource('');
            }}
            style={{
              ...actionButton,
              color: '#64748b',
            }}
          >
            Clear filters
          </button>
        )}
      </section>

      {/* LEADS */}
      {loading ? (
        <section
          style={{
            background: '#fff',
            border: '1px solid #e4eaf2',
            borderRadius: 14,
            padding: 60,
            textAlign: 'center',
            color: '#718096',
          }}
        >
          Loading leads...
        </section>
      ) : leads.length === 0 ? (
        <section
          style={{
            background: '#fff',
            border: '1px solid #e4eaf2',
            borderRadius: 14,
            padding: 65,
            textAlign: 'center',
          }}
        >
          <div
            style={{
              width: 52,
              height: 52,
              margin: '0 auto 14px',
              borderRadius: 16,
              display: 'grid',
              placeItems: 'center',
              background: '#edf4fc',
              color: '#315f93',
              fontSize: 22,
            }}
          >
            ◎
          </div>

          <h3
            style={{
              margin: 0,
              color: '#24354a',
              fontSize: 18,
            }}
          >
            {search || status || source
              ? 'No matching leads'
              : 'No leads yet'}
          </h3>

          <p
            style={{
              margin: '7px 0 18px',
              color: '#8793a3',
              fontSize: 12,
            }}
          >
            {search || status || source
              ? 'Try changing your search or filters.'
              : 'Add your first customer enquiry to get started.'}
          </p>

          {!search && !status && !source && (
            <button
              type="button"
              onClick={() => setModalLead(null)}
              style={{
                border: 0,
                borderRadius: 9,
                padding: '10px 16px',
                background: '#2459a6',
                color: '#fff',
                fontWeight: 700,
                cursor: 'pointer',
              }}
            >
              Add First Lead
            </button>
          )}
        </section>
      ) : (
        <section
          style={{
            display: 'grid',
            gridTemplateColumns:
              'repeat(auto-fill, minmax(320px, 1fr))',
            gap: 15,
          }}
        >
          {leads.map((lead) => {
            const color = statusColor(lead.status);

            return (
              <article
                key={lead.id}
                style={{
                  background: '#fff',
                  border: '1px solid #e4eaf2',
                  borderRadius: 15,
                  padding: 18,
                  boxShadow: '0 5px 18px rgba(31, 52, 80, .04)',
                  transition: 'transform .15s ease',
                }}
              >
                <header
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    gap: 12,
                    alignItems: 'flex-start',
                  }}
                >
                  <div style={{ minWidth: 0 }}>
                    <h3
                      style={{
                        margin: 0,
                        color: '#1c304a',
                        fontSize: 17,
                      }}
                    >
                      {lead.name}
                    </h3>

                    <p
                      style={{
                        margin: '5px 0 0',
                        color: '#8995a5',
                        fontSize: 11,
                      }}
                    >
                      {humanize(lead.source)}
                    </p>
                  </div>

                  <span
                    style={{
                      flexShrink: 0,
                      padding: '5px 9px',
                      borderRadius: 99,
                      background: `${color}15`,
                      color,
                      fontSize: 10,
                      fontWeight: 800,
                    }}
                  >
                    {humanize(lead.status)}
                  </span>
                </header>

                <div
                  style={{
                    marginTop: 18,
                    display: 'grid',
                    gap: 12,
                  }}
                >
                  <Info label="Phone" value={lead.phone || 'Not provided'} />
                  <Info label="Email" value={lead.email || 'Not provided'} />
                  <Info
                    label="Property"
                    value={
                      lead.interestedPropertyTitle ||
                      'No property selected'
                    }
                  />
                  <Info
                    label="Budget"
                    value={formatBudget(
                      lead.budgetMin,
                      lead.budgetMax,
                    )}
                  />
                  <Info
                    label="Location"
                    value={
                      lead.preferredLocation || 'Not specified'
                    }
                  />
                </div>

                <footer
                  style={{
                    display: 'flex',
                    gap: 8,
                    marginTop: 18,
                    paddingTop: 13,
                    borderTop: '1px solid #edf1f5',
                  }}
                >
                  <button
                    type="button"
                    style={actionButton}
                    onClick={() => setModalLead(lead)}
                  >
                    View / Edit
                  </button>

                  <button
                    type="button"
                    style={{
                      ...actionButton,
                      color: '#dc2626',
                    }}
                    onClick={() => deleteLeadRecord(lead)}
                  >
                    Delete
                  </button>
                </footer>
              </article>
            );
          })}
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

function Info({ label, value }) {
  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: '82px 1fr',
        gap: 8,
        alignItems: 'start',
      }}
    >
      <span
        style={{
          color: '#9aa6b5',
          fontSize: 10,
          fontWeight: 700,
        }}
      >
        {label}
      </span>

      <span
        style={{
          color: '#46566b',
          fontSize: 11,
          overflowWrap: 'anywhere',
        }}
      >
        {value}
      </span>
    </div>
  );
}