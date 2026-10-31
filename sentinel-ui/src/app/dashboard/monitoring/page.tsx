'use client';

import { RefreshCw } from 'lucide-react';
import { Button, Card, CardHeader, ErrorState, PageHeader, Skeleton, Stat, StatusDot } from '@/components/ui';
import { formatNumber } from '@/lib/format';
import { useSystemStatus } from '@/lib/queries';

export default function MonitoringPage() {
  const { data, isLoading, isFetching, error, refetch, dataUpdatedAt } = useSystemStatus();
  const metrics = data?.metrics;

  return (
    <>
      <PageHeader
        title="Monitoring"
        description="Live service health and gateway traffic. Refreshes every 15 seconds."
        actions={
          <Button onClick={() => refetch()} loading={isFetching}>
            {!isFetching && <RefreshCw className="h-4 w-4" aria-hidden />} Refresh
          </Button>
        }
      />

      {error ? (
        <Card><ErrorState message="Could not check system status." onRetry={() => refetch()} /></Card>
      ) : (
        <>
          <Card className="mb-6">
            <CardHeader title="Services" description="Health endpoints are probed from the dashboard server." />
            {isLoading ? <div className="p-5"><Skeleton className="h-16 w-full" /></div> : (
              <table className="w-full">
                <thead className="border-b border-slate-200 bg-slate-50/60 text-left text-xs font-medium uppercase tracking-wide text-slate-500">
                  <tr><th scope="col" className="px-5 py-3">Service</th><th scope="col" className="px-5 py-3">Status</th><th scope="col" className="px-5 py-3 text-right">Response time</th></tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {(data?.services ?? []).map((s) => (
                    <tr key={s.id}>
                      <td className="px-5 py-3.5 text-sm font-medium text-slate-900">{s.name}</td>
                      <td className="px-5 py-3.5"><StatusDot active={s.state === 'up'} labels={['Healthy', s.state === 'down' ? 'Unhealthy' : 'Unreachable']} /></td>
                      <td className="px-5 py-3.5 text-right text-sm tabular-nums text-slate-600">{s.latencyMs == null ? '—' : `${s.latencyMs} ms`}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </Card>

          <h2 className="mb-3 text-sm font-semibold text-slate-900">Gateway traffic <span className="font-normal text-slate-500">(5-minute average)</span></h2>
          {isLoading ? (
            <div className="grid gap-4 sm:grid-cols-3">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-24" />)}</div>
          ) : metrics?.available ? (
            <div className="grid gap-4 sm:grid-cols-3">
              <Stat label="Requests / min" value={formatNumber(Math.round(metrics.requestsPerMinute ?? 0))} />
              <Stat label="Throttled / min" value={formatNumber(Math.round(metrics.throttledPerMinute ?? 0))} hint="HTTP 429" />
              <Stat label="Server errors / min" value={formatNumber(Math.round(metrics.errorsPerMinute ?? 0))} hint="HTTP 5xx" />
            </div>
          ) : (
            <Card className="px-5 py-6 text-sm text-slate-600">
              Traffic metrics are not available. Set <code className="rounded bg-slate-100 px-1 font-mono text-xs">PROMETHEUS_URL</code> for the
              dashboard server and make sure Prometheus is scraping the gateway.
            </Card>
          )}

          {dataUpdatedAt > 0 && <p className="mt-4 text-xs text-slate-500">Last checked {new Date(dataUpdatedAt).toLocaleTimeString()}</p>}
        </>
      )}
    </>
  );
}
