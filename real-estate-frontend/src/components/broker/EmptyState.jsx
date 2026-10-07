export default function EmptyState({ icon, title, message }) {
  return (
    <div className="broker-empty-state">
      <span className="broker-empty-icon" aria-hidden="true">{icon}</span>
      <strong>{title}</strong>
      <p>{message}</p>
    </div>
  );
}
