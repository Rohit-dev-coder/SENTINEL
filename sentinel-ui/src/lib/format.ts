export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime())
    ? '—'
    : d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

export function formatNumber(n: number | null | undefined): string {
  return n == null ? '—' : n.toLocaleString();
}

export function maskKey(key: string): string {
  return key.length <= 8 ? '••••••••' : `${key.slice(0, 7)}••••••••••••`;
}

/** "100 per minute", "5 per 30 s" */
export function formatLimit(max: number, windowSeconds: number): string {
  if (windowSeconds === 60) return `${max.toLocaleString()} per minute`;
  if (windowSeconds === 3600) return `${max.toLocaleString()} per hour`;
  if (windowSeconds === 1) return `${max.toLocaleString()} per second`;
  return `${max.toLocaleString()} per ${windowSeconds} s`;
}

/** Clipboard API needs a secure context; fall back for plain-http dev access by IP. */
export async function copyText(text: string): Promise<boolean> {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
      return true;
    }
  } catch {
    /* fall through */
  }
  const area = document.createElement('textarea');
  area.value = text;
  area.style.position = 'fixed';
  area.style.opacity = '0';
  document.body.appendChild(area);
  area.select();
  const ok = document.execCommand('copy');
  document.body.removeChild(area);
  return ok;
}
