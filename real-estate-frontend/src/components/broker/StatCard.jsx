export default function StatCard({ label, icon, description, value }) {
  return (
    <article className="broker-stat-card">
      <div className="broker-stat-heading">
        <span>{label}</span>
        <span className="broker-stat-icon" aria-hidden="true">{icon}</span>
      </div>
      <p className={value === undefined ? 'broker-stat-empty' : 'broker-stat-value'}>
        {value === undefined ? 'No data available yet' : value}
      </p>
      <p className="broker-stat-description">{description}</p>
    </article>
  );
}
