import { useEffect, useState } from 'react';
import { fetchLeads, fetchPropertyMatches } from '../../api';

const cardStyle = {
  padding: 16,
  border: '1px solid #e6eaf0',
  borderRadius: 9,
  background: '#fff',
};
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

function money(value) {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(value);
}

function MatchingCard({ property }) {
  return (
    <article style={cardStyle}>
      <header style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12 }}>
        <div>
          <p style={{ margin: '0 0 4px', color: '#8492a5', fontSize: 9, fontWeight: 700, textTransform: 'uppercase' }}>
            {property.propertyType?.replaceAll('_', ' ')} · {property.listingType}
          </p>
          <h3 style={{ margin: 0, color: '#24354a', fontSize: 15 }}>{property.title}</h3>
        </div>
        {property.matchScore > 0 && (
          <span style={{ flex: '0 0 auto', padding: '5px 8px', borderRadius: 99, background: '#edf7ef', color: '#34744a', fontSize: 10, fontWeight: 700 }}>
            {property.matchScore}% match
          </span>
        )}
      </header>
      <p style={{ margin: '12px 0 8px', color: '#1f4d86', fontSize: 16, fontWeight: 700 }}>
        {money(property.price)}
      </p>
      <p style={{ margin: 0, color: '#66758a', fontSize: 11 }}>
        {property.location}, {property.city}, {property.state}
      </p>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 7, marginTop: 11 }}>
        {[
          `${property.bedrooms} bedrooms`,
          `${property.bathrooms} bathrooms`,
          `${new Intl.NumberFormat().format(property.area)} sq. ft.`,
          property.status,
        ].map((fact) => (
          <span key={fact} style={{ padding: '5px 7px', borderRadius: 5, background: '#f5f7fa', color: '#57667a', fontSize: 9 }}>
            {fact}
          </span>
        ))}
      </div>
      {property.matchReasons?.length > 0 && (
        <div style={{ marginTop: 13 }}>
          <strong style={{ color: '#526176', fontSize: 10 }}>Why it matches</strong>
          <ul style={{ display: 'flex', flexWrap: 'wrap', gap: '5px 16px', margin: '6px 0 0', paddingLeft: 17, color: '#66758a', fontSize: 10 }}>
            {property.matchReasons.map((reason) => <li key={reason}>{reason}</li>)}
          </ul>
        </div>
      )}
    </article>
  );
}

export default function PropertyMatching() {
  const [leads, setLeads] = useState([]);
  const [selectedLeadId, setSelectedLeadId] = useState('');
  const [matches, setMatches] = useState([]);
  const [loadingLeads, setLoadingLeads] = useState(true);
  const [loadingMatches, setLoadingMatches] = useState(false);
  const [error, setError] = useState('');
  const [leadsError, setLeadsError] = useState('');

  useEffect(() => {
    let cancelled = false;
    fetchLeads()
      .then((data) => {
        if (!cancelled) {
          setLeads(data);
          if (data.length) setSelectedLeadId(String(data[0].id));
        }
      })
      .catch((requestError) => {
        if (!cancelled) setLeadsError(requestError.message || 'Unable to load your leads.');
      })
      .finally(() => {
        if (!cancelled) setLoadingLeads(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!selectedLeadId) {
      setMatches([]);
      setError('');
      return undefined;
    }
    let cancelled = false;
    setLoadingMatches(true);
    setError('');
    fetchPropertyMatches(selectedLeadId)
      .then((data) => {
        if (!cancelled) setMatches(data);
      })
      .catch((requestError) => {
        if (!cancelled) setError(requestError.message || 'Unable to find matching properties.');
      })
      .finally(() => {
        if (!cancelled) setLoadingMatches(false);
      });
    return () => {
      cancelled = true;
    };
  }, [selectedLeadId]);

  return (
    <div style={{ display: 'grid', gap: 16, color: '#172033' }}>
      <header>
        <p style={{ margin: '0 0 3px', color: '#8492a5', fontSize: 9, fontWeight: 750, letterSpacing: 1 }}>
          BROKER WORKSPACE
        </p>
        <h2 style={{ margin: 0, color: '#1c2c40', fontSize: 23 }}>Property Matching</h2>
        <p style={{ margin: '4px 0 0', color: '#718096', fontSize: 12 }}>
          Find listings that fit a buyer’s requirements.
        </p>
      </header>

      <section style={{ ...cardStyle, display: 'grid', gap: 7 }}>
        <label htmlFor="matching-lead" style={{ color: '#526176', fontSize: 11, fontWeight: 650 }}>Select a lead</label>
        <select
          id="matching-lead"
          value={selectedLeadId}
          onChange={(event) => setSelectedLeadId(event.target.value)}
          disabled={loadingLeads || leads.length === 0}
          style={{ ...controlStyle, width: '100%', maxWidth: 500 }}
        >
          {loadingLeads && <option value="">Loading leads…</option>}
          {!loadingLeads && leads.length === 0 && <option value="">No leads available</option>}
          {leads.map((lead) => (
            <option key={lead.id} value={lead.id}>{lead.name}{lead.preferredLocation ? ` · ${lead.preferredLocation}` : ''}</option>
          ))}
        </select>
      </section>

      {leadsError && (
        <p role="alert" style={{ margin: 0, padding: 11, borderRadius: 7, background: '#fff5f5', color: '#a33636', fontSize: 11 }}>
          {leadsError}
        </p>
      )}
      {loadingMatches && (
        <section role="status" style={{ ...cardStyle, padding: 38, color: '#718096', textAlign: 'center', fontSize: 12 }}>
          Finding matching properties…
        </section>
      )}
      {!loadingMatches && error && (
        <p role="alert" style={{ margin: 0, padding: 11, borderRadius: 7, background: '#fff5f5', color: '#a33636', fontSize: 11 }}>
          {error}
        </p>
      )}
      {!loadingLeads && !leadsError && leads.length === 0 && (
        <section style={{ ...cardStyle, padding: 38, color: '#718096', textAlign: 'center', fontSize: 12 }}>
          Create a lead first to find matching properties.
        </section>
      )}
      {!loadingLeads && leads.length > 0 && !loadingMatches && !error && (
        matches.length ? (
          <section style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 310px), 1fr))', gap: 13 }} aria-label="Matching properties">
            {matches.map((property) => <MatchingCard key={property.id} property={property} />)}
          </section>
        ) : (
          <section style={{ ...cardStyle, padding: 38, color: '#718096', textAlign: 'center', fontSize: 12 }}>
            No matching properties found for this lead.
          </section>
        )
      )}
    </div>
  );
}
