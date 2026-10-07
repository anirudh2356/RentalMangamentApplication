import { useEffect, useMemo, useState } from 'react';
import { fetchCalendarEvents } from '../../api';
import './Calendar.css';

const weekdayLabels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

function dateKey(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function monthRange(month) {
  const first = new Date(month.getFullYear(), month.getMonth(), 1);
  const mondayOffset = (first.getDay() + 6) % 7;
  const gridStart = new Date(first.getFullYear(), first.getMonth(), first.getDate() - mondayOffset);
  const gridEnd = new Date(gridStart.getFullYear(), gridStart.getMonth(), gridStart.getDate() + 41);
  return { from: dateKey(gridStart), to: dateKey(gridEnd) };
}

function humanize(value) {
  return value
    ? value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (character) => character.toUpperCase())
    : '—';
}

function formatTime(value) {
  if (!value) return '';
  return value.slice(0, 5);
}

function eventDate(value) {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function EventDetails({ event, onClose }) {
  const information = [
    ['Type', humanize(event.type)],
    ['Date', eventDate(event.date).toLocaleDateString(undefined, { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })],
    ['Time (UTC)', formatTime(event.startTime) || 'Not specified'],
    ['Status', humanize(event.status)],
    ...(event.priority ? [['Priority', humanize(event.priority)]] : []),
    ['Related lead', event.relatedLeadName || '—'],
    ...(event.relatedPropertyTitle ? [['Related property', event.relatedPropertyTitle]] : []),
    ['Details', event.details || '—'],
  ];
  return (
    <div
      className="calendar-modal-backdrop"
      role="presentation"
      onMouseDown={(mouseEvent) => {
        if (mouseEvent.target === mouseEvent.currentTarget) onClose();
      }}
    >
      <section className="calendar-event-dialog" role="dialog" aria-modal="true" aria-labelledby="calendar-event-title">
        <div className="calendar-dialog-header">
          <span className={`calendar-event-type calendar-event-type--${event.type.toLowerCase()}`}>{humanize(event.type)}</span>
          <button type="button" className="calendar-icon-button" aria-label="Close event details" onClick={onClose}>×</button>
        </div>
        <h2 id="calendar-event-title">{event.title}</h2>
        <dl className="calendar-detail-list">
          {information.map(([label, value]) => (
            <div key={label}>
              <dt>{label}</dt>
              <dd>{value}</dd>
            </div>
          ))}
        </dl>
        <footer className="calendar-dialog-footer">
          <button type="button" className="calendar-action-button" onClick={onClose}>Close</button>
        </footer>
      </section>
    </div>
  );
}

export default function Calendar() {
  const [visibleMonth, setVisibleMonth] = useState(() => {
    const today = new Date();
    return new Date(today.getFullYear(), today.getMonth(), 1);
  });
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedEvent, setSelectedEvent] = useState(null);
  const range = useMemo(() => monthRange(visibleMonth), [visibleMonth]);

  useEffect(() => {
    let cancelled = false;
    fetchCalendarEvents(range.from, range.to)
      .then((data) => {
        if (!cancelled) {
          setEvents(data);
          setError('');
        }
      })
      .catch((requestError) => {
        if (!cancelled) setError(requestError.message || 'Unable to load calendar events.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [range]);

  const days = useMemo(() => {
    const { from } = range;
    const [year, month, day] = from.split('-').map(Number);
    const start = new Date(year, month - 1, day);
    return Array.from({ length: 42 }, (_, index) => {
      const date = new Date(start.getFullYear(), start.getMonth(), start.getDate() + index);
      return {
        date,
        key: dateKey(date),
        inMonth: date.getMonth() === visibleMonth.getMonth(),
        isToday: dateKey(date) === dateKey(new Date()),
      };
    });
  }, [range, visibleMonth]);

  const eventsByDate = useMemo(() => {
    const grouped = new Map();
    events.forEach((event) => {
      const daily = grouped.get(event.date) || [];
      daily.push(event);
      grouped.set(event.date, daily);
    });
    return grouped;
  }, [events]);

  function shiftMonth(amount) {
    setVisibleMonth((current) => new Date(current.getFullYear(), current.getMonth() + amount, 1));
    setLoading(true);
    setSelectedEvent(null);
  }

  function goToToday() {
    const today = new Date();
    setVisibleMonth(new Date(today.getFullYear(), today.getMonth(), 1));
    setLoading(true);
    setSelectedEvent(null);
  }

  return (
    <div className="broker-calendar">
      <header className="calendar-page-header">
        <div>
          <p className="calendar-kicker">SCHEDULE</p>
          <h2>Calendar</h2>
          <p>View your scheduled site visits and lead follow-ups</p>
        </div>
      </header>

      {error && <div className="calendar-alert" role="alert">{error}</div>}

      <section className="calendar-panel" aria-label="Broker calendar">
        <div className="calendar-toolbar">
          <div className="calendar-month-controls">
            <button type="button" className="calendar-icon-button" aria-label="Previous month" onClick={() => shiftMonth(-1)}>‹</button>
            <button type="button" className="calendar-icon-button" aria-label="Next month" onClick={() => shiftMonth(1)}>›</button>
            <h3 aria-live="polite">{visibleMonth.toLocaleDateString(undefined, { month: 'long', year: 'numeric' })}</h3>
          </div>
          <button type="button" className="calendar-action-button" onClick={goToToday}>Today</button>
        </div>

        <div className="calendar-legend" aria-label="Calendar event legend">
          <span><i className="calendar-legend-dot calendar-legend-dot--visit" />Site Visits</span>
          <span><i className="calendar-legend-dot calendar-legend-dot--followup" />Follow-ups</span>
          <small>Times are shown in UTC</small>
        </div>

        <div className="calendar-grid" role="grid" aria-label={visibleMonth.toLocaleDateString(undefined, { month: 'long', year: 'numeric' })}>
          {weekdayLabels.map((day) => (
            <div className="calendar-weekday" role="columnheader" key={day}>{day}</div>
          ))}
          {days.map(({ date, key, inMonth, isToday }) => {
            const dateEvents = eventsByDate.get(key) || [];
            return (
              <div
                className={`calendar-day${inMonth ? '' : ' calendar-day--outside'}${isToday ? ' calendar-day--today' : ''}`}
                role="gridcell"
                key={key}
                aria-label={`${date.toLocaleDateString()}${dateEvents.length ? `, ${dateEvents.length} events` : ''}`}
              >
                <span className="calendar-day-number">{date.getDate()}</span>
                <div className="calendar-day-events">
                  {dateEvents.map((event) => (
                    <button
                      type="button"
                      key={event.id}
                      className={`calendar-event calendar-event--${event.type.toLowerCase()}`}
                      title={`${formatTime(event.startTime)} ${event.title}`}
                      onClick={() => setSelectedEvent(event)}
                    >
                      {event.startTime && <time>{formatTime(event.startTime)}</time>}
                      <span>{event.title}</span>
                    </button>
                  ))}
                </div>
              </div>
            );
          })}
        </div>

        {loading && <div className="calendar-state" role="status">Loading calendar events…</div>}
        {!loading && !error && events.length === 0 && (
          <div className="calendar-state">
            <strong>No events this month</strong>
            <span>Scheduled site visits and follow-ups will appear here.</span>
          </div>
        )}
        {!loading && error && (
          <div className="calendar-state">
            <span>Calendar events could not be loaded.</span>
            <button type="button" className="calendar-action-button" onClick={() => {
              setLoading(true);
              setError('');
              fetchCalendarEvents(range.from, range.to)
                .then(setEvents)
                .catch((requestError) => setError(requestError.message || 'Unable to load calendar events.'))
                .finally(() => setLoading(false));
            }}>Try again</button>
          </div>
        )}
      </section>

      {selectedEvent && <EventDetails event={selectedEvent} onClose={() => setSelectedEvent(null)} />}
    </div>
  );
}
