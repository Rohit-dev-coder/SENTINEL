'use client';

import { useEffect, useId, useRef, useState } from 'react';
import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from 'react';
import { AlertCircle, Check, Copy, Loader2, X } from 'lucide-react';
import { copyText } from '@/lib/format';

const cx = (...parts: (string | false | null | undefined)[]) => parts.filter(Boolean).join(' ');

/* ---------- Button ---------- */

type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost';

const BUTTON_STYLES: Record<ButtonVariant, string> = {
  primary: 'bg-blue-600 text-white hover:bg-blue-700 focus-visible:outline-blue-600',
  secondary: 'bg-white text-slate-700 border border-slate-300 hover:bg-slate-50 focus-visible:outline-blue-600',
  danger: 'bg-red-600 text-white hover:bg-red-700 focus-visible:outline-red-600',
  ghost: 'text-slate-600 hover:bg-slate-100 focus-visible:outline-blue-600',
};

export function Button({
  variant = 'secondary',
  size = 'md',
  loading = false,
  className,
  children,
  disabled,
  ...rest
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: ButtonVariant; size?: 'sm' | 'md'; loading?: boolean }) {
  return (
    <button
      {...rest}
      disabled={disabled || loading}
      className={cx(
        'inline-flex items-center justify-center gap-2 rounded-md font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-50',
        size === 'sm' ? 'px-2.5 py-1.5 text-xs' : 'px-3.5 py-2 text-sm',
        BUTTON_STYLES[variant],
        className
      )}
    >
      {loading && <Loader2 className="h-4 w-4 animate-spin" aria-hidden />}
      {children}
    </button>
  );
}

/* ---------- Badge ---------- */

const BADGE_STYLES = {
  neutral: 'bg-slate-100 text-slate-700 ring-slate-200',
  blue: 'bg-blue-50 text-blue-700 ring-blue-200',
  green: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  amber: 'bg-amber-50 text-amber-800 ring-amber-200',
  red: 'bg-red-50 text-red-700 ring-red-200',
} as const;

export function Badge({ tone = 'neutral', children }: { tone?: keyof typeof BADGE_STYLES; children: ReactNode }) {
  return (
    <span className={cx('inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset', BADGE_STYLES[tone])}>
      {children}
    </span>
  );
}

export function StatusDot({ active, labels = ['Active', 'Inactive'] }: { active: boolean; labels?: [string, string] }) {
  return (
    <span className="inline-flex items-center gap-1.5 text-sm text-slate-700">
      <span className={cx('h-2 w-2 rounded-full', active ? 'bg-emerald-500' : 'bg-slate-300')} aria-hidden />
      {active ? labels[0] : labels[1]}
    </span>
  );
}

/* ---------- Layout bits ---------- */

export function PageHeader({ title, description, actions }: { title: string; description?: string; actions?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
      <div>
        <h1 className="text-xl font-semibold tracking-tight text-slate-900">{title}</h1>
        {description && <p className="mt-1 text-sm text-slate-500">{description}</p>}
      </div>
      {actions && <div className="flex items-center gap-2">{actions}</div>}
    </div>
  );
}

export function Card({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={cx('rounded-lg border border-slate-200 bg-white shadow-sm', className)}>{children}</div>;
}

export function CardHeader({ title, description, action }: { title: string; description?: string; action?: ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4 border-b border-slate-200 px-5 py-4">
      <div>
        <h2 className="text-sm font-semibold text-slate-900">{title}</h2>
        {description && <p className="mt-0.5 text-xs text-slate-500">{description}</p>}
      </div>
      {action}
    </div>
  );
}

export function Stat({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <Card className="px-5 py-4">
      <p className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-2 text-2xl font-semibold tabular-nums text-slate-900">{value}</p>
      {hint && <p className="mt-1 text-xs text-slate-500">{hint}</p>}
    </Card>
  );
}

export function Skeleton({ className }: { className?: string }) {
  return <div className={cx('animate-pulse rounded bg-slate-200/70', className)} />;
}

export function TableSkeleton({ rows = 5, cols = 5 }: { rows?: number; cols?: number }) {
  return (
    <div className="divide-y divide-slate-100">
      {Array.from({ length: rows }).map((_, r) => (
        <div key={r} className="flex gap-6 px-5 py-4">
          {Array.from({ length: cols }).map((__, c) => (
            <Skeleton key={c} className="h-4 flex-1" />
          ))}
        </div>
      ))}
    </div>
  );
}

export function EmptyState({ icon, title, description, action }: { icon: ReactNode; title: string; description: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center px-6 py-14 text-center">
      <div className="mb-3 flex h-11 w-11 items-center justify-center rounded-full bg-slate-100 text-slate-500">{icon}</div>
      <h3 className="text-sm font-semibold text-slate-900">{title}</h3>
      <p className="mt-1 max-w-sm text-sm text-slate-500">{description}</p>
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div role="alert" className="flex flex-col items-center px-6 py-14 text-center">
      <div className="mb-3 flex h-11 w-11 items-center justify-center rounded-full bg-red-50 text-red-600">
        <AlertCircle className="h-5 w-5" />
      </div>
      <h3 className="text-sm font-semibold text-slate-900">Could not load data</h3>
      <p className="mt-1 max-w-sm text-sm text-slate-500">{message}</p>
      {onRetry && (
        <Button className="mt-4" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  );
}

/* ---------- Form fields ---------- */

const FIELD_BASE =
  'block rounded-md border bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 disabled:bg-slate-50 disabled:text-slate-500';

function fieldClass(error?: string, className?: string) {
  return cx(
    FIELD_BASE,
    // Full width unless the caller sets its own width (e.g. toolbar filters)
    !/(^|\s)w-/.test(className ?? '') && 'w-full',
    error ? 'border-red-400 focus:ring-red-200' : 'border-slate-300 focus:border-blue-500 focus:ring-blue-100',
    className
  );
}

export function Field({
  label,
  error,
  hint,
  children,
  htmlFor,
}: {
  label: string;
  error?: string;
  hint?: string;
  children: ReactNode;
  htmlFor: string;
}) {
  return (
    <div>
      <label htmlFor={htmlFor} className="mb-1.5 block text-sm font-medium text-slate-700">
        {label}
      </label>
      {children}
      {error ? (
        <p className="mt-1 text-xs text-red-600" role="alert">
          {error}
        </p>
      ) : (
        hint && <p className="mt-1 text-xs text-slate-500">{hint}</p>
      )}
    </div>
  );
}

export function Input({ error, ...rest }: InputHTMLAttributes<HTMLInputElement> & { error?: string }) {
  return <input {...rest} aria-invalid={!!error} className={fieldClass(error, rest.className)} />;
}

export function Select({ error, children, ...rest }: SelectHTMLAttributes<HTMLSelectElement> & { error?: string }) {
  return (
    <select {...rest} aria-invalid={!!error} className={fieldClass(error, rest.className)}>
      {children}
    </select>
  );
}

export { cx };

/* ---------- Modal (native <dialog>: focus trap + Escape for free) ---------- */

export function Modal({
  open,
  onClose,
  title,
  description,
  children,
  footer,
  size = 'md',
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children: ReactNode;
  footer?: ReactNode;
  size?: 'md' | 'lg';
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const titleId = useId();

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  return (
    <dialog
      ref={ref}
      aria-labelledby={titleId}
      onClose={onClose}
      onClick={(e) => {
        if (e.target === ref.current) onClose();
      }}
      className={cx(
        'm-auto w-full rounded-xl p-0 shadow-2xl backdrop:bg-slate-900/40 backdrop:backdrop-blur-[1px]',
        size === 'lg' ? 'max-w-2xl' : 'max-w-lg'
      )}
    >
      {open && (
        <div>
          <div className="flex items-start justify-between gap-4 border-b border-slate-200 px-6 py-4">
            <div>
              <h2 id={titleId} className="text-base font-semibold text-slate-900">
                {title}
              </h2>
              {description && <p className="mt-0.5 text-sm text-slate-500">{description}</p>}
            </div>
            <button onClick={onClose} aria-label="Close" className="rounded p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600">
              <X className="h-5 w-5" />
            </button>
          </div>
          <div className="px-6 py-5">{children}</div>
          {footer && <div className="flex justify-end gap-2 border-t border-slate-200 bg-slate-50 px-6 py-3 rounded-b-xl">{footer}</div>}
        </div>
      )}
    </dialog>
  );
}

export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel,
  loading,
  onConfirm,
  onClose,
}: {
  open: boolean;
  title: string;
  message: ReactNode;
  confirmLabel: string;
  loading?: boolean;
  onConfirm: () => void;
  onClose: () => void;
}) {
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      footer={
        <>
          <Button onClick={onClose} disabled={loading}>
            Cancel
          </Button>
          <Button variant="danger" onClick={onConfirm} loading={loading}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      <p className="text-sm text-slate-600">{message}</p>
    </Modal>
  );
}

/* ---------- Copy button ---------- */

export function CopyButton({ text, label = 'Copy' }: { text: string; label?: string }) {
  const [copied, setCopied] = useState(false);
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      onClick={async () => {
        if (await copyText(text)) {
          setCopied(true);
          setTimeout(() => setCopied(false), 1500);
        }
      }}
      className="rounded p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-700"
    >
      {copied ? <Check className="h-4 w-4 text-emerald-600" /> : <Copy className="h-4 w-4" />}
    </button>
  );
}

/* ---------- Table primitives ---------- */

export function Th({ children, className }: { children?: ReactNode; className?: string }) {
  return (
    <th scope="col" className={cx('px-5 py-3 text-left text-xs font-medium uppercase tracking-wide text-slate-500', className)}>
      {children}
    </th>
  );
}

export function Td({ children, className }: { children?: ReactNode; className?: string }) {
  return <td className={cx('px-5 py-3.5 text-sm text-slate-700', className)}>{children}</td>;
}
