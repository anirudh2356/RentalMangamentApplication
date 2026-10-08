import { useEffect, useState } from 'react';
import {
  fetchCommissions,
  fetchDeals,
  fetchLeads,
  fetchProperties,
  fetchSiteVisits,
} from '../../api';
import EmptyState from '../../components/broker/EmptyState';
import PipelineCard from '../../components/broker/PipelineCard';
import StatCard from '../../components/broker/StatCard';

const statistics = [
  ['Total Leads', '◎', 'People interested in your listings'],
  ['Active Listings', '⌂', 'Properties currently on the market'],
  ['Upcoming Site Visits', '▣', 'Scheduled property visits'],
  ['Active Deals', '◇', 'Deals currently in progress'],
  ['Closed Deals', '✓', 'Successfully completed deals'],
  ['Total Commission', '$', 'Commission earned to date'],
  ['Pending Commission', '◷', 'Commission awaiting payment'],
];

const stages = [
  'New',
  'Contacted',
  'Site Visit',
  'Negotiation',
  'Documentation',
  'Closed',
];

export default function BrokerDashboard({ onNavigate }) {
  const [listingMetric, setListingMetric] = useState({
    loading: true,
    count: null,
    error: false,
  });

  const [dealMetrics, setDealMetrics] = useState({
    loading: true,
    active: null,
    won: null,
    error: false,
  });

  const [commissionMetrics, setCommissionMetrics] = useState({
    loading: true,
    total: null,
    pending: null,
    error: false,
  });

  const [activityMetrics, setActivityMetrics] = useState({
    loading: true,
    leads: 0,
    visits: 0,
    error: false,
  });

  useEffect(() => {
    let cancelled = false;

    fetchProperties()
      .then((properties) => {
        if (!cancelled) {
          setListingMetric({
            loading: false,
            count: Array.isArray(properties)
              ? properties.filter(
                  (property) => property.status === 'ACTIVE'
                ).length
              : 0,
            error: false,
          });
        }
      })
      .catch(() => {
        if (!cancelled) {
          setListingMetric({
            loading: false,
            count: null,
            error: true,
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;

    fetchCommissions()
      .then((summary) => {
        if (!cancelled) {
          setCommissionMetrics({
            loading: false,
            total: summary.totalCommission,
            pending: summary.pendingCommission,
            error: false,
          });
        }
      })
      .catch(() => {
        if (!cancelled) {
          setCommissionMetrics({
            loading: false,
            total: null,
            pending: null,
            error: true,
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;

    Promise.all([
      fetchLeads(),
      fetchSiteVisits('SCHEDULED'),
    ])
      .then(([leads, visits]) => {
        if (!cancelled) {
          setActivityMetrics({
            loading: false,
            leads: Array.isArray(leads) ? leads.length : 0,
            visits: Array.isArray(visits) ? visits.length : 0,
            error: false,
          });
        }
      })
      .catch(() => {
        if (!cancelled) {
          setActivityMetrics({
            loading: false,
            leads: 0,
            visits: 0,
            error: true,
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;

    fetchDeals()
      .then((deals) => {
        if (!cancelled) {
          const safeDeals = Array.isArray(deals) ? deals : [];

          setDealMetrics({
            loading: false,
            active: safeDeals.filter(
              (deal) => deal.status === 'ACTIVE'
            ).length,
            won: safeDeals.filter(
              (deal) => deal.status === 'WON'
            ).length,
            error: false,
          });
        }
      })
      .catch(() => {
        if (!cancelled) {
          setDealMetrics({
            loading: false,
            active: null,
            won: null,
            error: true,
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  function commissionValue(value) {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(value ?? 0);
  }

  function metricValue(label) {
    if (label === 'Total Leads') {
      return activityMetrics.loading
        ? 'Loading…'
        : activityMetrics.error
          ? 'Unavailable'
          : activityMetrics.leads;
    }

    if (label === 'Active Listings') {
      return listingMetric.loading
        ? 'Loading…'
        : listingMetric.error
          ? 'Unavailable'
          : listingMetric.count;
    }

    if (label === 'Upcoming Site Visits') {
      return activityMetrics.loading
        ? 'Loading…'
        : activityMetrics.error
          ? 'Unavailable'
          : activityMetrics.visits;
    }

    if (label === 'Active Deals') {
      return dealMetrics.loading
        ? 'Loading…'
        : dealMetrics.error
          ? 'Unavailable'
          : dealMetrics.active;
    }

    if (label === 'Closed Deals') {
      return dealMetrics.loading
        ? 'Loading…'
        : dealMetrics.error
          ? 'Unavailable'
          : dealMetrics.won;
    }

    if (label === 'Total Commission') {
      return commissionMetrics.loading
        ? 'Loading…'
        : commissionMetrics.error
          ? 'Unavailable'
          : commissionValue(commissionMetrics.total);
    }

    if (label === 'Pending Commission') {
      return commissionMetrics.loading
        ? 'Loading…'
        : commissionMetrics.error
          ? 'Unavailable'
          : commissionValue(commissionMetrics.pending);
    }

    return undefined;
  }

  return (
    <div className="broker-dashboard">

      {/* HERO */}
      <section className="broker-hero">
        <div className="broker-hero-copy">
          <span className="broker-hero-kicker">
            BROKER WORKSPACE
          </span>

          <h2>
            Your business,{' '}
            <span>beautifully organised.</span>
          </h2>

          <p>
            Manage leads, listings, visits and deals from one focused
            workspace.
          </p>
        </div>

        <div className="broker-hero-meta">
          <span
            className="broker-live-dot"
            aria-hidden="true"
          />

          <span>Workspace live</span>

          <time>
            {new Date().toLocaleDateString(undefined, {
              weekday: 'short',
              day: 'numeric',
              month: 'short',
              year: 'numeric',
            })}
          </time>
        </div>
      </section>

      {/* STATISTICS */}
      <section
        className="broker-stat-grid"
        aria-label="Business metrics"
      >
        {statistics.map(([label, icon, description]) => (
          <StatCard
            key={label}
            label={label}
            icon={icon}
            description={description}
            value={metricValue(label)}
          />
        ))}
      </section>

      {/* SALES PIPELINE */}
      <section className="broker-panel broker-pipeline">
        <div className="broker-panel-heading">
          <div>
            <h2>Sales pipeline</h2>
            <p>
              Track progress from first contact through closing.
            </p>
          </div>

          <span className="broker-live-pill">
            LIVE WORKFLOW
          </span>
        </div>

        <div className="broker-pipeline-grid">
          {stages.map((stage, index) => (
            <PipelineCard
              key={stage}
              title={stage}
              stageNumber={String(index + 1).padStart(2, '0')}
            />
          ))}
        </div>
      </section>

      {/* QUICK ACTIONS */}
      <section className="broker-panel broker-actions-panel">
        <div className="broker-panel-heading">
          <div>
            <h2>Quick actions</h2>
            <p>
              Jump straight into the work that matters.
            </p>
          </div>
        </div>

        <div className="broker-quick-actions">
          {[
            ['Add lead', '◎', 'leads'],
            ['Add property', '⌂', 'properties'],
            ['Schedule visit', '▣', 'site-visits'],
            ['Create deal', '◇', 'deals'],
          ].map(([action, icon, target]) => (
            <button
              type="button"
              key={action}
              onClick={() => onNavigate?.(target)}
            >
              <span aria-hidden="true">{icon}</span>
              <strong>{action}</strong>
              <small>Open workspace →</small>
            </button>
          ))}
        </div>
      </section>

      {/* BUSINESS PULSE */}
      <section className="broker-pulse">
        <div className="broker-pulse-heading">
          <div>
            <span className="broker-section-kicker">
              BUSINESS PULSE
            </span>

            <h2>What needs your attention</h2>
          </div>

          <span className="broker-pulse-note">
            Live from your workspace
          </span>
        </div>

        <div className="broker-pulse-grid">

          <div className="broker-pulse-item">
            <span className="broker-pulse-icon">
              ◎
            </span>

            <div>
              <strong>
                {activityMetrics.loading ||
                activityMetrics.error
                  ? '—'
                  : activityMetrics.leads}
              </strong>

              <span>Active leads</span>
            </div>

            <button
              type="button"
              onClick={() => onNavigate?.('leads')}
            >
              View
            </button>
          </div>

          <div className="broker-pulse-item">
            <span className="broker-pulse-icon">
              ▣
            </span>

            <div>
              <strong>
                {activityMetrics.loading ||
                activityMetrics.error
                  ? '—'
                  : activityMetrics.visits}
              </strong>

              <span>Scheduled visits</span>
            </div>

            <button
              type="button"
              onClick={() =>
                onNavigate?.('site-visits')
              }
            >
              View
            </button>
          </div>

          <div className="broker-pulse-item">
            <span className="broker-pulse-icon">
              ◇
            </span>

            <div>
              <strong>
                {dealMetrics.loading ||
                dealMetrics.error
                  ? '—'
                  : dealMetrics.active}
              </strong>

              <span>Deals in progress</span>
            </div>

            <button
              type="button"
              onClick={() => onNavigate?.('deals')}
            >
              View
            </button>
          </div>

        </div>
      </section>

      {/* LOWER OVERVIEW */}
      <div className="broker-overview-grid">

        <section className="broker-panel">
          <div className="broker-panel-heading">
            <div>
              <h2>Upcoming site visits</h2>
              <p>
                Your next scheduled property appointments.
              </p>
            </div>
          </div>

          <EmptyState
            icon="▣"
            title="No upcoming site visits"
            message="Scheduled visits will appear here."
          />
        </section>

        <section className="broker-panel">
          <div className="broker-panel-heading">
            <div>
              <h2>Recent leads</h2>
              <p>
                New contacts and recent enquiries.
              </p>
            </div>
          </div>

          <EmptyState
            icon="◎"
            title="No leads available"
            message="New leads will appear here."
          />
        </section>

        <section className="broker-panel broker-recent-deals">
          <div className="broker-panel-heading">
            <div>
              <h2>Recent deals</h2>
              <p>
                Updates on your latest transactions.
              </p>
            </div>
          </div>

          <EmptyState
            icon="◇"
            title="No deals available"
            message="Deal activity will appear here."
          />
        </section>

      </div>
    </div>
  );
}