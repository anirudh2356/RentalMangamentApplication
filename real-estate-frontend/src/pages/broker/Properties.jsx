import { useCallback, useEffect, useState } from 'react';

import {
  createProperty,
  deactivateProperty,
  fetchProperties,
  updateProperty,
} from '../../api';

import './Properties.css';

const initialForm = {
  title: '',
  description: '',
  propertyType: 'APARTMENT',
  listingType: 'SALE',
  price: '',
  area: '',
  bedrooms: '0',
  bathrooms: '0',
  location: '',
  city: '',
  state: '',
  pincode: '',
  furnishingStatus: 'UNFURNISHED',
  parkingAvailable: false,
  available: true,
  status: 'ACTIVE',
};

const propertyTypes = [
  'APARTMENT',
  'HOUSE',
  'VILLA',
  'PLOT',
  'COMMERCIAL',
  'OFFICE',
  'SHOP',
];

const listingTypes = ['SALE', 'RENT'];

const statuses = [
  'ACTIVE',
  'INACTIVE',
  'SOLD',
  'RENTED',
];

const furnishingStatuses = [
  'FURNISHED',
  'SEMI_FURNISHED',
  'UNFURNISHED',
];

function displayEnum(value) {
  return (
    value
      ?.replaceAll('_', ' ')
      .toLowerCase()
      .replace(/\b\w/g, (character) => character.toUpperCase()) ||
    'Not specified'
  );
}

function formatPrice(price, listingType) {
  const amount = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(price || 0);

  return listingType === 'RENT'
    ? `${amount} / month`
    : amount;
}

function formFromProperty(property) {
  return Object.fromEntries(
    Object.keys(initialForm).map((key) => [
      key,
      property[key] ?? initialForm[key],
    ]),
  );
}

function PropertyForm({
  mode,
  property,
  onClose,
  onSave,
}) {
  const readOnly = mode === 'view';

  const [form, setForm] = useState(
    property
      ? formFromProperty(property)
      : { ...initialForm },
  );

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  function setField(event) {
    const {
      name,
      value,
      type,
      checked,
    } = event.target;

    setForm((current) => ({
      ...current,
      [name]:
        type === 'checkbox'
          ? checked
          : value,
    }));
  }

  async function submit(event) {
    event.preventDefault();

    if (readOnly) {
      onClose();
      return;
    }

    setError('');
    setSaving(true);

    const payload = {
      ...form,
      title: form.title.trim(),
      description: form.description.trim(),
      location: form.location.trim(),
      city: form.city.trim(),
      state: form.state.trim(),
      pincode: form.pincode.trim(),
      price: Number(form.price),
      area: Number(form.area),
      bedrooms: Number(form.bedrooms),
      bathrooms: Number(form.bathrooms),
    };

    try {
      await onSave(payload);
    } catch (saveError) {
      setError(
        saveError.message ||
          'Unable to save this property.',
      );
    } finally {
      setSaving(false);
    }
  }

  function textField(label, name, props = {}) {
    return (
      <label className="property-field">
        <span>{label}</span>

        <input
          name={name}
          value={form[name]}
          onChange={setField}
          disabled={readOnly}
          required={props.required ?? true}
          maxLength={props.maxLength}
          min={props.min}
          step={props.step}
          type={props.type || 'text'}
          placeholder={props.placeholder}
        />
      </label>
    );
  }

  function selectField(label, name, options) {
    return (
      <label className="property-field">
        <span>{label}</span>

        <select
          name={name}
          value={form[name]}
          onChange={setField}
          disabled={readOnly}
        >
          {options.map((option) => (
            <option
              key={option}
              value={option}
            >
              {displayEnum(option)}
            </option>
          ))}
        </select>
      </label>
    );
  }

  return (
    <div
      className="property-modal-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (
          event.target === event.currentTarget
        ) {
          onClose();
        }
      }}
    >
      <section
        className="property-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="property-modal-title"
      >
        <header className="property-modal-header">
          <div>
            <p className="property-kicker">
              PROPERTY LISTING
            </p>

            <h2 id="property-modal-title">
              {mode === 'create'
                ? 'Add property'
                : mode === 'edit'
                  ? 'Edit property'
                  : 'Property details'}
            </h2>
          </div>

          <button
            type="button"
            className="property-icon-button"
            onClick={onClose}
            aria-label="Close"
          >
            ×
          </button>
        </header>

        <form onSubmit={submit}>
          <div className="property-form-grid">
            {textField(
              'Title',
              'title',
              {
                maxLength: 160,
                placeholder:
                  'e.g. Bright 2-bedroom apartment',
              },
            )}

            {selectField(
              'Property type',
              'propertyType',
              propertyTypes,
            )}

            {selectField(
              'Listing type',
              'listingType',
              listingTypes,
            )}

            {textField(
              'Price (₹)',
              'price',
              {
                type: 'number',
                min: '0.01',
                step: '0.01',
              },
            )}

            {textField(
              'Area (sq. ft.)',
              'area',
              {
                type: 'number',
                min: '0.01',
                step: '0.01',
              },
            )}

            {textField(
              'Bedrooms',
              'bedrooms',
              {
                type: 'number',
                min: '0',
                step: '1',
              },
            )}

            {textField(
              'Bathrooms',
              'bathrooms',
              {
                type: 'number',
                min: '0',
                step: '1',
              },
            )}

            {textField(
              'Location',
              'location',
              {
                maxLength: 255,
                placeholder:
                  'Street, neighborhood or area',
              },
            )}

            {textField(
              'City',
              'city',
              {
                maxLength: 100,
              },
            )}

            {textField(
              'State',
              'state',
              {
                maxLength: 100,
              },
            )}

            {textField(
              'Pincode',
              'pincode',
              {
                maxLength: 20,
                required: false,
              },
            )}

            {selectField(
              'Furnishing',
              'furnishingStatus',
              furnishingStatuses,
            )}

            {selectField(
              'Status',
              'status',
              statuses,
            )}

            <label className="property-checkbox">
              <input
                type="checkbox"
                name="parkingAvailable"
                checked={Boolean(
                  form.parkingAvailable,
                )}
                onChange={setField}
                disabled={readOnly}
              />

              <span>
                Parking available
              </span>
            </label>

            <label className="property-checkbox">
              <input
                type="checkbox"
                name="available"
                checked={Boolean(
                  form.available,
                )}
                onChange={setField}
                disabled={readOnly}
              />

              <span>
                Currently available
              </span>
            </label>

            <label className="property-field property-description">
              <span>Description</span>

              <textarea
                name="description"
                value={form.description}
                onChange={setField}
                disabled={readOnly}
                minLength={10}
                maxLength={5000}
                rows={4}
                required
                placeholder="Describe the property's key features and details."
              />
            </label>
          </div>

          {error && (
            <p
              className="property-form-error"
              role="alert"
            >
              {error}
            </p>
          )}

          <footer className="property-modal-actions">
            <button
              type="button"
              className="property-button property-button-secondary"
              onClick={onClose}
            >
              {readOnly
                ? 'Close'
                : 'Cancel'}
            </button>

            {!readOnly && (
              <button
                type="submit"
                className="property-button property-button-primary"
                disabled={saving}
              >
                {saving
                  ? 'Saving…'
                  : mode === 'create'
                    ? 'Add property'
                    : 'Save changes'}
              </button>
            )}
          </footer>
        </form>
      </section>
    </div>
  );
}

function PropertyCard({
  property,
  onView,
  onEdit,
  onDeactivate,
}) {
  return (
    <article className="property-card">
      <div
        className="property-image-placeholder"
        aria-label="No property image available"
      >
        <span aria-hidden="true">
          ⌂
        </span>
      </div>

      <div className="property-card-body">
        <div className="property-card-title-row">
          <div>
            <p className="property-card-type">
              {displayEnum(
                property.propertyType,
              )}{' '}
              ·{' '}
              {displayEnum(
                property.listingType,
              )}
            </p>

            <h2>
              {property.title}
            </h2>
          </div>

          <span
            className={`property-status property-status-${property.status?.toLowerCase()}`}
          >
            {displayEnum(
              property.status,
            )}
          </span>
        </div>

        <strong className="property-card-price">
          {formatPrice(
            property.price,
            property.listingType,
          )}
        </strong>

        <p className="property-card-location">
          ⌖ {property.location},{' '}
          {property.city},{' '}
          {property.state}
        </p>

        <div className="property-card-facts">
          <span>
            {property.bedrooms} bed
          </span>

          <span>
            {property.bathrooms} bath
          </span>

          <span>
            {new Intl.NumberFormat(
              'en-IN',
            ).format(
              property.area || 0,
            )}{' '}
            sq. ft.
          </span>
        </div>

        <div className="property-card-footer">
          <span>
            Added{' '}
            {property.createdAt
              ? new Date(
                  property.createdAt,
                ).toLocaleDateString(
                  'en-IN',
                )
              : 'Recently'}
          </span>

          <div className="property-card-actions">
            <button
              type="button"
              onClick={() =>
                onView(property)
              }
            >
              View
            </button>

            <button
              type="button"
              onClick={() =>
                onEdit(property)
              }
            >
              Edit
            </button>

            {property.status ===
              'ACTIVE' && (
              <button
                type="button"
                className="property-delete-action"
                onClick={() =>
                  onDeactivate(property)
                }
              >
                Deactivate
              </button>
            )}
          </div>
        </div>
      </div>
    </article>
  );
}

export default function Properties({
  csrfToken,
}) {
  const [properties, setProperties] =
    useState([]);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState('');

  const [success, setSuccess] =
    useState('');

  const [query, setQuery] =
    useState('');

  const [typeFilter, setTypeFilter] =
    useState('');

  const [listingFilter, setListingFilter] =
    useState('');

  const [statusFilter, setStatusFilter] =
    useState('');

  const [modal, setModal] =
    useState(null);

  const loadProperties =
    useCallback(async () => {
      setLoading(true);
      setError('');

      try {
        const data =
          await fetchProperties();

        setProperties(
          Array.isArray(data)
            ? data
            : [],
        );
      } catch (loadError) {
        setError(
          loadError.message ||
            'Unable to load properties.',
        );
      } finally {
        setLoading(false);
      }
    }, []);

  useEffect(() => {
    loadProperties();
  }, [loadProperties]);

  useEffect(() => {
    if (!success) return;

    const timer =
      setTimeout(() => {
        setSuccess('');
      }, 3500);

    return () =>
      clearTimeout(timer);
  }, [success]);

  async function saveProperty(
    payload,
  ) {
    setError('');

    if (modal.mode === 'create') {
      await createProperty(
        payload,
        csrfToken,
      );

      setSuccess(
        'Property added successfully.',
      );
    } else {
      await updateProperty(
        modal.property.id,
        payload,
        csrfToken,
      );

      setSuccess(
        'Property updated successfully.',
      );
    }

    setModal(null);

    await loadProperties();
  }

  async function handleDeactivate(
    property,
  ) {
    const confirmed =
      window.confirm(
        `Deactivate "${property.title}"? It will remain in your listings as inactive.`,
      );

    if (!confirmed) return;

    setSuccess('');
    setError('');

    try {
      await deactivateProperty(
        property.id,
        csrfToken,
      );

      setSuccess(
        'Property deactivated.',
      );

      await loadProperties();
    } catch (
      deactivateError
    ) {
      setError(
        deactivateError.message ||
          'Unable to deactivate this property.',
      );
    }
  }

  const filteredProperties =
    properties.filter(
      (property) => {
        const search = query
          .trim()
          .toLowerCase();

        const matchesSearch =
          !search ||
          [
            property.title,
            property.city,
            property.location,
            property.state,
          ].some((value) =>
            value
              ?.toString()
              .toLowerCase()
              .includes(search),
          );

        return (
          matchesSearch &&
          (!typeFilter ||
            property.propertyType ===
              typeFilter) &&
          (!listingFilter ||
            property.listingType ===
              listingFilter) &&
          (!statusFilter ||
            property.status ===
              statusFilter)
        );
      },
    );

  return (
    <div className="properties-page">
      <header className="properties-page-header">
        <div>
          <p className="property-kicker">
            LISTINGS
          </p>

          <h2>
            Properties
          </h2>

          <p>
            Manage your property
            listings from one place.
          </p>
        </div>

        <button
          type="button"
          className="property-button property-button-primary"
          onClick={() =>
            setModal({
              mode: 'create',
            })
          }
        >
          <span aria-hidden="true">
            ＋
          </span>

          Add Property
        </button>
      </header>

      {success && (
        <div
          className="property-notice property-notice-success"
          role="status"
        >
          ✓ {success}
        </div>
      )}

      {error && (
        <div
          className="property-notice property-notice-error"
          role="alert"
        >
          {error}
        </div>
      )}

      <section
        className="property-filters"
        aria-label="Filter properties"
      >
        <label className="property-search">
          <span aria-hidden="true">
            ⌕
          </span>

          <input
            type="search"
            placeholder="Search title, city or location"
            value={query}
            onChange={(event) =>
              setQuery(
                event.target.value,
              )
            }
            aria-label="Search title, city or location"
          />
        </label>

        <select
          aria-label="Filter by property type"
          value={typeFilter}
          onChange={(event) =>
            setTypeFilter(
              event.target.value,
            )
          }
        >
          <option value="">
            All property types
          </option>

          {propertyTypes.map(
            (type) => (
              <option
                key={type}
                value={type}
              >
                {displayEnum(type)}
              </option>
            ),
          )}
        </select>

        <select
          aria-label="Filter by listing type"
          value={listingFilter}
          onChange={(event) =>
            setListingFilter(
              event.target.value,
            )
          }
        >
          <option value="">
            Sale and rent
          </option>

          {listingTypes.map(
            (type) => (
              <option
                key={type}
                value={type}
              >
                {displayEnum(type)}
              </option>
            ),
          )}
        </select>

        <select
          aria-label="Filter by status"
          value={statusFilter}
          onChange={(event) =>
            setStatusFilter(
              event.target.value,
            )
          }
        >
          <option value="">
            All statuses
          </option>

          {statuses.map(
            (status) => (
              <option
                key={status}
                value={status}
              >
                {displayEnum(status)}
              </option>
            ),
          )}
        </select>
      </section>

      {loading ? (
        <div
          className="properties-state"
          role="status"
        >
          <span className="property-spinner" />
          Loading properties…
        </div>
      ) : filteredProperties.length > 0 ? (
        <section
          className="property-card-grid"
          aria-label="Your properties"
        >
          {filteredProperties.map(
            (property) => (
              <PropertyCard
                key={property.id}
                property={property}
                onView={(selected) =>
                  setModal({
                    mode: 'view',
                    property: selected,
                  })
                }
                onEdit={(selected) =>
                  setModal({
                    mode: 'edit',
                    property: selected,
                  })
                }
                onDeactivate={
                  handleDeactivate
                }
              />
            ),
          )}
        </section>
      ) : properties.length === 0 ? (
        <div className="properties-empty">
          <span
            className="properties-empty-icon"
            aria-hidden="true"
          >
            ⌂
          </span>

          <h2>
            No properties listed yet
          </h2>

          <p>
            Add your first property
            to start managing your
            listings.
          </p>

          <button
            type="button"
            className="property-button property-button-primary"
            onClick={() =>
              setModal({
                mode: 'create',
              })
            }
          >
            Add your first property
          </button>
        </div>
      ) : (
        <div className="properties-state">
          No properties match the
          selected search and filters.
        </div>
      )}

      {modal && (
        <PropertyForm
          key={`${modal.mode}-${
            modal.property?.id ||
            'new'
          }`}
          mode={modal.mode}
          property={modal.property}
          onClose={() =>
            setModal(null)
          }
          onSave={saveProperty}
        />
      )}
    </div>
  );
}