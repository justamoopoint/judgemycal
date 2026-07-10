import type { Confidence, MealEstimate } from "../domain/models";

const BAND_COLORS: Record<Confidence, string> = {
  HIGH: "var(--mint-deep)",
  MEDIUM: "var(--amber)",
  LOW: "var(--lavender)",
};

export function ConfidencePill({ confidence }: { confidence: Confidence }) {
  return <span className={`pill ${confidence}`}>{confidence}</span>;
}

/**
 * The signature visual: the calorie total drawn as a band from low to high
 * with the point estimate inside it. The range IS the answer.
 */
export function ConfidenceBand({ estimate }: { estimate: MealEstimate }) {
  const span = Math.max(1, estimate.totalHigh - estimate.totalLow);
  const point = Math.min(
    100,
    Math.max(0, ((estimate.totalKcal - estimate.totalLow) / span) * 100),
  );
  const color = BAND_COLORS[estimate.overallConfidence];
  return (
    <div>
      <p className="total" data-testid="estimate_total">
        ~{estimate.totalKcal} kcal
      </p>
      <div className="range" data-testid="estimate_range">
        {estimate.totalLow}–{estimate.totalHigh} kcal
      </div>
      <div className="band-track" style={{ marginTop: 8 }}>
        <div
          className="band-fill"
          style={{ width: `${point}%`, background: `color-mix(in srgb, ${color} 45%, transparent)` }}
        />
        <div className="band-fill" style={{ width: 6, background: color }} />
        <div
          className="band-fill"
          style={{ flex: 1, background: `color-mix(in srgb, ${color} 45%, transparent)` }}
        />
      </div>
      <div className="row" style={{ justifyContent: "flex-start", marginTop: 8 }}>
        <ConfidencePill confidence={estimate.overallConfidence} />
        {estimate.lowestConfidenceItem && (
          <span className="muted">least sure about: {estimate.lowestConfidenceItem}</span>
        )}
      </div>
    </div>
  );
}

/**
 * The break-character moment. Calm, plain, unmissable — no persona styling,
 * because the persona has deliberately stepped aside.
 */
export function SupportDialog({
  message,
  onDismiss,
}: {
  message: string;
  onDismiss: () => void;
}) {
  return (
    <div className="dialog-backdrop" role="alertdialog" aria-modal="true">
      <div className="dialog">
        <strong>Before anything else —</strong>
        <p data-testid="support_message" style={{ margin: 0 }}>
          {message}
        </p>
        <button className="primary" data-testid="support_dismiss" onClick={onDismiss}>
          Okay
        </button>
      </div>
    </div>
  );
}

export function OfflineBadge() {
  return (
    <span className="badge-offline" data-testid="offline_badge">
      Offline estimate
    </span>
  );
}
