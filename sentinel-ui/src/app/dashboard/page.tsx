'use client';

import Link from 'next/link';
import { ArrowRight } from 'lucide-react';
import { Badge, Card, CardHeader, ErrorState, PageHeader, Skeleton, Stat, StatusDot } from '@/components/ui';
import { toProblem } from '@/lib/api';
import { formatDate, formatNumber } from '@/lib/format';
import { useRules, useSystemStatus, useTenants } from '@/lib/queries';
import { ALGORITHMS, ALGORITHM_INFO, TIERS } from '@/types';

export default function OverviewPage() {
  const tenants = useTenants();
  const rules = useRules();
  const status = useSystemStatus();

  const loading = tenants.isLoading || rules.isLoading;
  const error = tenants.error ?? rules.error;

  const t = tenants.data ?? [];
  const r = rules.data ?? [];
  const recent = [...t].sort((a, b) => b.createdAt.localeCompare(a.createdAt)).slice(0, 5);
  const maxTier = Math.max(1, ...TIERS.map((tier) => t.filter((x) => x.tier === tier).length));
  const maxAlgo = Math.max(1, ...ALGORITHMS.map((a) => r.filter((x) => x.algorithm === a).length));

  return (
    <>
      <PageHeader title="Overview" description="Configuration at a glance and the health of the platform." />

      {error ? (
        <Card><ErrorState message={toProblem(error).message} onRetry={() => { tenants.refetch(); rules.refetch(); }} /></Card>
      ) : (
        <>
          <div className="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
            {loading ? (
              Array.from({ length: 4 }).map((_, i) => <Card key={i} className="px-5 py-4"><Skeleton className="h-3 w-20" /><Skeleton className="mt-3 h-7 w-12" /></Card>)
            ) : (
              <>
                <Stat label="Tenants" value={formatNumber(t.length)} hint={`${t.filter((x) => x.isActive).length} active`} />
                <Stat label="Custom rules" value={formatNumber(r.length)} hint={`${r.filter((x) => x.isActive).length} active`} />
                <Stat label="Requests / min" value={status.data?.metrics.available ? formatNumber(Math.round(status.data.metrics.requestsPerMinute ?? 0)) : '—'} hint={status.data?.metrics.available ? '5-minute average' : 'Metrics not connected'} />
                <Stat label="Throttled / min" value={status.data?.metrics.available ? formatNumber(Math.round(status.data.metrics.throttledPerMinute ?? 0)) : '—'} hint={status.data?.metrics.available ? 'HTTP 429 responses' : 'Metrics not connected'} />
              </>
            )}
          </div>

          <div className="mb-6 grid gap-6 lg:grid-cols-2">
            <Card>
              <CardHeader title="Tenants by tier" />
              <div className="space-y-3 px-5 py-4">
                {loading ? <Skeleton className="h-24 w-full" /> : TIERS.map((tier) => {
                  const n = t.filter((x) => x.tier === tier).length;
                  return (
                    <div key={tier} className="flex items-center gap-3 text-sm">
                      <span className="w-24 text-slate-600">{tier[0] + tier.slice(1).toLowerCase()}</span>
                      <div className="h-2 flex-1 rounded-full bg-slate-100"><div className="h-2 rounded-full bg-blue-600" style={{ width: `${(n / maxTier) * 100}%` }} /></div>
                      <span className="w-6 text-right tabular-nums text-slate-900">{n}</span>
                    </div>
                  );
                })}
              </div>
            </Card>
            <Card>
              <CardHeader title="Rules by algorithm" />
              <div className="space-y-3 px-5 py-4">
                {loading ? <Skeleton className="h-24 w-full" /> : ALGORITHMS.map((a) => {
                  const n = r.filter((x) => x.algorithm === a).length;
                  return (
                    <div key={a} className="flex items-center gap-3 text-sm">
                      <span className="w-44 text-slate-600">{ALGORITHM_INFO[a].label}</span>
                      <div className="h-2 flex-1 rounded-full bg-slate-100"><div className="h-2 rounded-full bg-slate-700" style={{ width: `${(n / maxAlgo) * 100}%` }} /></div>
                      <span className="w-6 text-right tabular-nums text-slate-900">{n}</span>
                    </div>
                  );
                })}
              </div>
            </Card>
          </div>

          <div className="grid gap-6 lg:grid-cols-2">
            <Card>
              <CardHeader title="Recently added tenants" action={<Link href="/dashboard/tenants" className="inline-flex items-center gap-1 text-xs font-medium text-blue-600 hover:text-blue-700">View all <ArrowRight className="h-3 w-3" aria-hidden /></Link>} />
              {loading ? <div className="p-5"><Skeleton className="h-24 w-full" /></div> : recent.length === 0 ? (
                <p className="px-5 py-8 text-center text-sm text-slate-500">No tenants yet.</p>
              ) : (
                <ul className="divide-y divide-slate-100">
                  {recent.map((x) => (
                    <li key={x.id} className="flex items-center justify-between px-5 py-3">
                      <div><p className="text-sm font-medium text-slate-900">{x.name}</p><p className="text-xs text-slate-500">Added {formatDate(x.createdAt)}</p></div>
                      <div className="flex items-center gap-3"><Badge tone="blue">{x.tier[0] + x.tier.slice(1).toLowerCase()}</Badge><StatusDot active={x.isActive} /></div>
                    </li>
                  ))}
                </ul>
              )}
            </Card>
            <Card>
              <CardHeader title="Service health" action={<Link href="/dashboard/monitoring" className="inline-flex items-center gap-1 text-xs font-medium text-blue-600 hover:text-blue-700">Details <ArrowRight className="h-3 w-3" aria-hidden /></Link>} />
              {status.isLoading ? <div className="p-5"><Skeleton className="h-16 w-full" /></div> : (
                <ul className="divide-y divide-slate-100">
                  {(status.data?.services ?? []).map((s) => (
                    <li key={s.id} className="flex items-center justify-between px-5 py-3">
                      <span className="text-sm font-medium text-slate-900">{s.name}</span>
                      <StatusDot active={s.state === 'up'} labels={['Healthy', s.state === 'down' ? 'Unhealthy' : 'Unreachable']} />
                    </li>
                  ))}
                </ul>
              )}
            </Card>
          </div>
        </>
      )}
    </>
  );
}
