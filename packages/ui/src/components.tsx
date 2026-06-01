import type { ButtonHTMLAttributes, PropsWithChildren, ReactNode } from "react";

type ButtonProps = PropsWithChildren<ButtonHTMLAttributes<HTMLButtonElement> & {
  kind?: "primary" | "secondary" | "ghost";
  fullWidth?: boolean;
}>;

export function Button({ children, className = "", kind = "primary", fullWidth = false, type = "button", ...props }: ButtonProps) {
  return (
    <button
      {...props}
      className={`dt-button dt-button--${kind} ${fullWidth ? "dt-button--block" : ""} ${className}`.trim()}
      type={type}
    >
      {children}
    </button>
  );
}

export function SurfaceCard({
  title,
  eyebrow,
  children,
  actions,
}: PropsWithChildren<{
  title?: string;
  eyebrow?: string;
  actions?: ReactNode;
}>) {
  return (
    <section className="dt-card">
      {(title || eyebrow || actions) && (
        <header className="dt-card__header">
          <div>
            {eyebrow ? <p className="dt-card__eyebrow">{eyebrow}</p> : null}
            {title ? <h3 className="dt-card__title">{title}</h3> : null}
          </div>
          {actions ? <div>{actions}</div> : null}
        </header>
      )}
      {children}
    </section>
  );
}

export function SectionHeading({
  title,
  description,
  badge,
}: {
  title: string;
  description: string;
  badge?: string;
}) {
  return (
    <div className="dt-section-heading">
      {badge ? <span className="dt-badge">{badge}</span> : null}
      <h2>{title}</h2>
      <p>{description}</p>
    </div>
  );
}

export function MetricCard({
  label,
  value,
  trend,
}: {
  label: string;
  value: string;
  trend?: string;
}) {
  return (
    <article className="dt-metric-card">
      <span className="dt-metric-card__label">{label}</span>
      <strong className="dt-metric-card__value">{value}</strong>
      {trend ? <span className="dt-metric-card__trend">{trend}</span> : null}
    </article>
  );
}

export function StatusState({
  title,
  description,
  tone = "neutral",
  action,
}: {
  title: string;
  description: string;
  tone?: "neutral" | "error" | "success";
  action?: ReactNode;
}) {
  return (
    <div className={`dt-status-state dt-status-state--${tone}`}>
      <h3>{title}</h3>
      <p>{description}</p>
      {action ? <div className="dt-status-state__action">{action}</div> : null}
    </div>
  );
}

export function Tag({ children, tone = "default" }: PropsWithChildren<{ tone?: "default" | "accent" | "success" | "warning" }>) {
  return <span className={`dt-tag dt-tag--${tone}`}>{children}</span>;
}

export function DataList({
  columns,
  rows,
}: {
  columns: string[];
  rows: Array<Array<ReactNode>>;
}) {
  return (
    <div className="dt-table">
      <div className="dt-table__row dt-table__row--head">
        {columns.map((column) => (
          <span key={column}>{column}</span>
        ))}
      </div>
      {rows.map((row, rowIndex) => (
        <div className="dt-table__row" key={`row-${rowIndex}`}>
          {row.map((cell, cellIndex) => (
            <span key={`cell-${rowIndex}-${cellIndex}`}>{cell}</span>
          ))}
        </div>
      ))}
    </div>
  );
}
