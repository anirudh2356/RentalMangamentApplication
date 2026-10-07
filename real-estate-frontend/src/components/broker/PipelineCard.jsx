export default function PipelineCard({ title, stageNumber }) {
  return (
    <section className="broker-pipeline-card" aria-label={`${title} pipeline stage`}>
      <div className="broker-pipeline-card-heading">
        <span>{title}</span>
        <span className="broker-pipeline-stage-number">{stageNumber}</span>
      </div>
      <p>No deals in this stage</p>
    </section>
  );
}
