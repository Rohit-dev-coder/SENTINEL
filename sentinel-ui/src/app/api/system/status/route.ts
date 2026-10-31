import type { ServiceState, SystemStatus } from '@/types';

// Probed from the server so the browser never needs direct access to the (internal) management ports.
const TARGETS = [
  { id: 'admin', name: 'Admin service', url: process.env.ADMIN_HEALTH_URL || 'http://localhost:9081/actuator/health' },
  { id: 'gateway', name: 'Gateway', url: process.env.GATEWAY_HEALTH_URL || 'http://localhost:9080/actuator/health' },
];

async function probe(url: string): Promise<{ state: ServiceState; latencyMs: number | null }> {
  const started = Date.now();
  try {
    const response = await fetch(url, { cache: 'no-store', signal: AbortSignal.timeout(3000) });
    const body = (await response.json().catch(() => ({}))) as { status?: string };
    return { state: response.ok && body.status === 'UP' ? 'up' : 'down', latencyMs: Date.now() - started };
  } catch {
    return { state: 'unreachable', latencyMs: null };
  }
}

/** Instant PromQL query; null when Prometheus is not configured, unreachable or has no data. */
async function query(base: string, promql: string): Promise<number | null> {
  try {
    const response = await fetch(`${base}/api/v1/query?query=${encodeURIComponent(promql)}`, {
      cache: 'no-store',
      signal: AbortSignal.timeout(3000),
    });
    if (!response.ok) return null;
    const body = (await response.json()) as { data?: { result?: { value?: [number, string] }[] } };
    const raw = body.data?.result?.[0]?.value?.[1];
    const value = raw === undefined ? NaN : Number(raw);
    return Number.isFinite(value) ? value : null;
  } catch {
    return null;
  }
}

export async function GET() {
  const prometheus = process.env.PROMETHEUS_URL?.replace(/\/$/, '');
  const metric = 'spring_cloud_gateway_requests_seconds_count';

  const [probes, rpm, throttled, errors] = await Promise.all([
    Promise.all(TARGETS.map((t) => probe(t.url))),
    prometheus ? query(prometheus, `sum(rate(${metric}[5m]))*60`) : null,
    prometheus ? query(prometheus, `sum(rate(${metric}{httpStatusCode="429"}[5m]))*60`) : null,
    prometheus ? query(prometheus, `sum(rate(${metric}{httpStatusCode=~"5.."}[5m]))*60`) : null,
  ]);

  const status: SystemStatus = {
    services: TARGETS.map((t, i) => ({ id: t.id, name: t.name, ...probes[i] })),
    metrics: {
      available: rpm !== null,
      requestsPerMinute: rpm,
      throttledPerMinute: throttled ?? 0,
      errorsPerMinute: errors ?? 0,
    },
    checkedAt: new Date().toISOString(),
  };
  return Response.json(status, { headers: { 'Cache-Control': 'no-store' } });
}
