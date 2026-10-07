import { useEffect, useState } from 'react';
import {
  deleteDocument,
  downloadDocument,
  fetchDeals,
  fetchDocuments,
  fetchLeads,
  fetchProperties,
  uploadDocument,
  updateDocument,
} from '../../api';
import './Documents.css';

const DOCUMENT_TYPES = [
  'PROPERTY_DOCUMENT',
  'AGREEMENT',
  'IDENTITY_DOCUMENT',
  'PAYMENT_DOCUMENT',
  'DEAL_DOCUMENT',
  'SITE_VISIT_DOCUMENT',
  'OTHER',
];

const emptyForm = {
  file: null,
  documentType: 'OTHER',
  description: '',
  propertyId: '',
  leadId: '',
  dealId: '',
};

function humanize(value) {
  return value
    ? value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (character) => character.toUpperCase())
    : '—';
}

function formatFileSize(size) {
  if (!size) return '0 bytes';
  if (size < 1024) return `${size} bytes`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(value) {
  return value ? new Date(value).toLocaleDateString() : '—';
}

function optionLabel(record, type) {
  if (type === 'property') return `${record.title} · ${record.city}`;
  if (type === 'lead') return record.name;
  return `${record.leadName} · ${record.propertyTitle}`;
}

export default function Documents({ csrfToken }) {
  const [documents, setDocuments] = useState([]);
  const [properties, setProperties] = useState([]);
  const [leads, setLeads] = useState([]);
  const [deals, setDeals] = useState([]);
  const [filters, setFilters] = useState({
    search: '',
    documentType: '',
    propertyId: '',
    leadId: '',
    dealId: '',
  });
  const [form, setForm] = useState(emptyForm);
  const [editingDocument, setEditingDocument] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [actionId, setActionId] = useState(null);
  const [loadError, setLoadError] = useState('');
  const [actionError, setActionError] = useState('');
  const [linkedDataError, setLinkedDataError] = useState('');
  const [reload, setReload] = useState(0);

  useEffect(() => {
    let cancelled = false;
    fetchDocuments(filters)
      .then((data) => {
        if (!cancelled) {
          setDocuments(data);
          setLoadError('');
        }
      })
      .catch((error) => {
        if (!cancelled) setLoadError(error.message || 'Unable to load documents.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [filters, reload]);

  useEffect(() => {
    let cancelled = false;
    Promise.all([fetchProperties(), fetchLeads(), fetchDeals()])
      .then(([propertyRows, leadRows, dealRows]) => {
        if (!cancelled) {
          setProperties(propertyRows);
          setLeads(leadRows);
          setDeals(dealRows);
        }
      })
      .catch((error) => {
        if (!cancelled) setLinkedDataError(error.message || 'Linked records could not be loaded.');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  function updateFilter(name, value) {
    setLoading(true);
    setFilters((current) => ({ ...current, [name]: value }));
  }

  function openUpload() {
    setEditingDocument(null);
    setForm(emptyForm);
    setActionError('');
    setModalOpen(true);
  }

  function openEdit(document) {
    setEditingDocument(document);
    setForm({
      ...emptyForm,
      documentType: document.documentType,
      description: document.description || '',
      propertyId: document.propertyId ? String(document.propertyId) : '',
      leadId: document.leadId ? String(document.leadId) : '',
      dealId: document.dealId ? String(document.dealId) : '',
    });
    setActionError('');
    setModalOpen(true);
  }

  async function submitForm(event) {
    event.preventDefault();
    setSubmitting(true);
    setActionError('');
    try {
      if (editingDocument) {
        await updateDocument(editingDocument.id, {
          documentType: form.documentType,
          description: form.description.trim() || null,
          propertyId: form.propertyId || null,
          leadId: form.leadId || null,
          dealId: form.dealId || null,
        }, csrfToken);
      } else {
        const formData = new FormData();
        formData.append('file', form.file);
        formData.append('documentType', form.documentType);
        if (form.description.trim()) formData.append('description', form.description.trim());
        if (form.propertyId) formData.append('propertyId', form.propertyId);
        if (form.leadId) formData.append('leadId', form.leadId);
        if (form.dealId) formData.append('dealId', form.dealId);
        await uploadDocument(formData, csrfToken);
      }
      setModalOpen(false);
      setLoading(true);
      setReload((count) => count + 1);
    } catch (error) {
      setActionError(error.message || 'The document could not be saved.');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDownload(document) {
    setActionError('');
    setActionId(document.id);
    try {
      const blob = await downloadDocument(document.id);
      const url = URL.createObjectURL(blob);
      const link = window.document.createElement('a');
      link.href = url;
      link.download = document.originalFileName;
      link.click();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (error) {
      setActionError(error.message || 'The document could not be downloaded.');
    } finally {
      setActionId(null);
    }
  }

  async function handleDelete(document) {
    if (!window.confirm(`Delete "${document.originalFileName}"? This also removes the stored file.`)) return;
    setActionError('');
    setActionId(document.id);
    try {
      await deleteDocument(document.id, csrfToken);
      setDocuments((current) => current.filter((item) => item.id !== document.id));
    } catch (error) {
      setActionError(error.message || 'The document could not be deleted.');
    } finally {
      setActionId(null);
    }
  }

  return (
    <main className="broker-documents">
      <header className="documents-header">
        <div>
          <p className="documents-kicker">WORKSPACE</p>
          <h2>Documents</h2>
          <p>Store and manage files linked to your properties, leads, and deals.</p>
        </div>
        <button className="documents-primary-button" type="button" onClick={openUpload}>＋ Upload document</button>
      </header>

      <section className="documents-filters" aria-label="Document filters">
        <input
          aria-label="Search documents"
          type="search"
          placeholder="Search file name or description"
          value={filters.search}
          onChange={(event) => updateFilter('search', event.target.value)}
        />
        <select aria-label="Filter by document type" value={filters.documentType} onChange={(event) => updateFilter('documentType', event.target.value)}>
          <option value="">All document types</option>
          {DOCUMENT_TYPES.map((type) => <option key={type} value={type}>{humanize(type)}</option>)}
        </select>
        <select aria-label="Filter by property" value={filters.propertyId} onChange={(event) => updateFilter('propertyId', event.target.value)}>
          <option value="">All properties</option>
          {properties.map((item) => <option key={item.id} value={item.id}>{item.title}</option>)}
        </select>
        <select aria-label="Filter by lead" value={filters.leadId} onChange={(event) => updateFilter('leadId', event.target.value)}>
          <option value="">All leads</option>
          {leads.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
        </select>
        <select aria-label="Filter by deal" value={filters.dealId} onChange={(event) => updateFilter('dealId', event.target.value)}>
          <option value="">All deals</option>
          {deals.map((item) => <option key={item.id} value={item.id}>{optionLabel(item, 'deal')}</option>)}
        </select>
      </section>

      {loadError && (
        <section className="documents-alert" role="alert">
          <span>{loadError}</span>
          <button type="button" onClick={() => {
            setLoading(true);
            setReload((count) => count + 1);
          }}>Try again</button>
        </section>
      )}
      {actionError && <p className="documents-alert" role="alert">{actionError}</p>}
      {linkedDataError && <p className="documents-note" role="status">{linkedDataError} Linked-record filters and selections may be incomplete.</p>}

      {loading ? (
        <section className="documents-state" role="status">Loading documents…</section>
      ) : loadError ? (
        <section className="documents-state">Documents could not be loaded.</section>
      ) : documents.length ? (
        <section className="documents-list" aria-label="Documents">
          {documents.map((document) => (
            <article className="document-card" key={document.id}>
              <div className="document-file-mark" aria-hidden="true">▤</div>
              <div className="document-main">
                <h3 title={document.originalFileName}>{document.originalFileName}</h3>
                <div className="document-tags">
                  <span className="document-type-tag">{humanize(document.documentType)}</span>
                  {document.propertyTitle && <span>Property: {document.propertyTitle}</span>}
                  {document.leadName && <span>Lead: {document.leadName}</span>}
                  {document.dealSummary && <span>Deal: {document.dealSummary}</span>}
                </div>
                {document.description && <p className="document-description">{document.description}</p>}
              </div>
              <div className="document-meta">
                <span>{formatFileSize(document.fileSize)}</span>
                <span>Uploaded {formatDate(document.uploadedAt)}</span>
              </div>
              <div className="document-actions">
                <button type="button" disabled={actionId === document.id} onClick={() => handleDownload(document)}>Download</button>
                <button type="button" disabled={actionId === document.id} onClick={() => openEdit(document)}>Edit</button>
                <button className="document-delete-button" type="button" disabled={actionId === document.id} onClick={() => handleDelete(document)}>Delete</button>
              </div>
            </article>
          ))}
        </section>
      ) : (
        <section className="documents-state documents-empty">
          <span aria-hidden="true">▤</span>
          <h3>{Object.values(filters).some(Boolean) ? 'No documents match these filters' : 'No documents uploaded yet'}</h3>
          <p>{Object.values(filters).some(Boolean)
            ? 'Try adjusting your search or clearing one or more filters.'
            : 'Upload a file to keep important property, lead, or deal documents together.'}</p>
          {!Object.values(filters).some(Boolean) && <button className="documents-primary-button" type="button" onClick={openUpload}>Upload your first document</button>}
        </section>
      )}

      {modalOpen && (
        <div className="documents-modal-backdrop" role="presentation" onMouseDown={(event) => {
          if (event.target === event.currentTarget && !submitting) setModalOpen(false);
        }}>
          <section className="documents-modal" role="dialog" aria-modal="true" aria-labelledby="documents-modal-title">
            <header>
              <div>
                <p className="documents-kicker">{editingDocument ? 'DOCUMENT DETAILS' : 'FILE UPLOAD'}</p>
                <h3 id="documents-modal-title">{editingDocument ? 'Edit document' : 'Upload document'}</h3>
              </div>
              <button type="button" aria-label="Close dialog" disabled={submitting} onClick={() => setModalOpen(false)}>×</button>
            </header>
            <form onSubmit={submitForm}>
              {editingDocument ? (
                <div className="documents-selected-file">{editingDocument.originalFileName}</div>
              ) : (
                <label className="documents-field">
                  <span>File <strong>*</strong></span>
                  <input
                    type="file"
                    accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
                    required
                    onChange={(event) => setForm((current) => ({ ...current, file: event.target.files?.[0] || null }))}
                  />
                  <small>PDF, DOC, DOCX, JPG, or PNG · maximum 10 MB</small>
                </label>
              )}
              <label className="documents-field">
                <span>Document type <strong>*</strong></span>
                <select required value={form.documentType} onChange={(event) => setForm((current) => ({ ...current, documentType: event.target.value }))}>
                  {DOCUMENT_TYPES.map((type) => <option key={type} value={type}>{humanize(type)}</option>)}
                </select>
              </label>
              <label className="documents-field">
                <span>Description</span>
                <textarea maxLength={2000} rows={3} value={form.description} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} />
              </label>
              <div className="documents-related-fields">
                <label className="documents-field">
                  <span>Property (optional)</span>
                  <select value={form.propertyId} onChange={(event) => setForm((current) => ({ ...current, propertyId: event.target.value }))}>
                    <option value="">Not linked</option>
                    {properties.map((item) => <option key={item.id} value={item.id}>{optionLabel(item, 'property')}</option>)}
                  </select>
                </label>
                <label className="documents-field">
                  <span>Lead (optional)</span>
                  <select value={form.leadId} onChange={(event) => setForm((current) => ({ ...current, leadId: event.target.value }))}>
                    <option value="">Not linked</option>
                    {leads.map((item) => <option key={item.id} value={item.id}>{optionLabel(item, 'lead')}</option>)}
                  </select>
                </label>
                <label className="documents-field">
                  <span>Deal (optional)</span>
                  <select value={form.dealId} onChange={(event) => setForm((current) => ({ ...current, dealId: event.target.value }))}>
                    <option value="">Not linked</option>
                    {deals.map((item) => <option key={item.id} value={item.id}>{optionLabel(item, 'deal')}</option>)}
                  </select>
                </label>
              </div>
              {actionError && <p className="documents-alert" role="alert">{actionError}</p>}
              <footer>
                <button type="button" disabled={submitting} onClick={() => setModalOpen(false)}>Cancel</button>
                <button className="documents-primary-button" type="submit" disabled={submitting || (!editingDocument && !form.file)}>
                  {submitting ? (editingDocument ? 'Saving…' : 'Uploading…') : (editingDocument ? 'Save changes' : 'Upload')}
                </button>
              </footer>
            </form>
          </section>
        </div>
      )}
    </main>
  );
}
