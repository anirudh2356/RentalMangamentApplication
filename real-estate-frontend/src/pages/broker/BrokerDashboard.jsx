import { useEffect, useState } from 'react';
import { fetchCommissions, fetchDeals, fetchProperties } from '../../api';
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

const actions = [
  'Add lead',
  'Add property',
  'Schedule site visit',
  'Create deal',
];

export default function BrokerDashboard() {
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

  useEffect(() => {
    let cancelled = false;
    fetchProperties()
      .then((properties) => {
        if (!cancelled) {
          setListingMetric({
            loading: false,
            count: properties.filter((property) => property.status === 'ACTIVE').length,
            error: false,
          });
        }
      })
      .catch(() => {
        if (!cancelled) {
          setListingMetric({ loading: false, count: null, error: true });
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
          setCommissionMetrics({ loading: false, total: null, pending: null, error: true });
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
    }).format(value);
  }

  useEffect(() => {
    let cancelled = false;
    fetchDeals()
      .then((deals) => {
        if (!cancelled) {
          setDealMetrics({
            loading: false,
            active: deals.filter((deal) => deal.status === 'ACTIVE').length,
            won: deals.filter((deal) => deal.status === 'WON').length,
            error: false,
          });
        }
      })
      .catch(() => {
        if (!cancelled) {
          setDealMetrics({ loading: false, active: null, won: null, error: true });
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="broker-dashboard">
      <section className="broker-welcome">
        <div>
          <p className="broker-section-kicker">OVERVIEW</p>
          <h2>Your business at a glance</h2>
          <p>Keep track of your real-estate work from one workspace.</p>
        </div>
        <span className="broker-date-note">
          Business metrics will appear here as your workspace grows.
        </span>
      </section>

      <section className="broker-stat-grid" aria-label="Business metrics">
        {statistics.map(([label, icon, description]) => (
          <StatCard
            key={label}
            label={label}
            icon={icon}
            description={description}
            value={
              label === 'Active Listings'
                ? listingMetric.loading
                  ? 'Loading…'
                  : listingMetric.error
                    ? 'Unavailable'
                    : listingMetric.count
                : label === 'Active Deals'
                  ? dealMetrics.loading
                    ? 'Loading…'
                    : dealMetrics.error
                      ? 'Unavailable'
                      : dealMetrics.active
                  : label === 'Closed Deals'
                    ? dealMetrics.loading
                      ? 'Loading…'
                      : dealMetrics.error
                        ? 'Unavailable'
                        : dealMetrics.won
                    : label === 'Total Commission'
                      ? commissionMetrics.loading
                        ? 'Loading…'
                        : commissionMetrics.error
                          ? 'Unavailable'
                          : commissionValue(commissionMetrics.total)
                      : label === 'Pending Commission'
                        ? commissionMetrics.loading
                          ? 'Loading…'
                          : commissionMetrics.error
                            ? 'Unavailable'
                            : commissionValue(commissionMetrics.pending)
                        : undefined
            }
          />
        ))}
      </section>

      <section className="broker-panel broker-pipeline">
        <div className="broker-panel-heading">
          <div>
            <h2>Sales pipeline</h2>
            <p>Track progress from first contact through closing.</p>
          </div>
          <span className="broker-coming-soon-label">Pipeline setup coming soon</span>
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

      <section className="broker-panel">
        <div className="broker-panel-heading">
          <div>
            <h2>Quick actions</h2>
            <p>Shortcuts for common broker tasks.</p>
          </div>
        </div>
        <div className="broker-quick-actions">
          {actions.map((action) => (
            <button type="button" key={action} disabled title="Coming soon">
              <span aria-hidden="true">＋</span>
              {action}
              <small>Coming soon</small>
            </button>
          ))}
        </div>
      </section>

      <div className="broker-overview-grid">
        <section className="broker-panel">
          <div className="broker-panel-heading">
            <div>
              <h2>Upcoming site visits</h2>
              <p>Your next scheduled property appointments.</p>
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
              <p>New contacts and recent enquiries.</p>
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
              <p>Updates on your latest transactions.</p>
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
