'use client';

import { useMemo, useState } from 'react';
import { MoreHorizontal, Pencil, Plus, Search, SlidersHorizontal, Trash2 } from 'lucide-react';
import {
  Badge, Button, Card, CardHeader, ConfirmDialog, EmptyState, ErrorState, Field, Input, Modal, PageHeader,
  Select, StatusDot, TableSkeleton, Td, Th,
} from '@/components/ui';
import { useToast } from '@/components/toast';
import { toProblem } from '@/lib/api';
import { formatLimit } from '@/lib/format';
import {
  useCreateRule, useDeleteRule, useRules, useSetRuleActive, useTenants, useUpdateRule,
} from '@/lib/queries';
import { ALGORITHMS, ALGORITHM_INFO, TIERS, TIER_DEFAULTS } from '@/types';
import type { Algorithm, Rule, Tenant } from '@/types';

export default function RateLimitsPage() {
  const toast = useToast();
  const rulesQuery = useRules();
  const tenantsQuery = useTenants();
  const setActive = useSetRuleActive();
  const remove = useDeleteRule();

  const [search, setSearch] = useState('');
  const [tenantFilter, setTenantFilter] = useState('ALL');
  const [algoFilter, setAlgoFilter] = useState<'ALL' | Algorithm>('ALL');
  const [formTarget, setFormTarget] = useState<Rule | 'new' | null>(null);
  const [toDelete, setToDelete] = useState<Rule | null>(null);
  const [menuFor, setMenuFor] = useState<string | null>(null);

  const tenants = useMemo(() => tenantsQuery.data ?? [], [tenantsQuery.data]);
  const tenantName = useMemo(() => new Map(tenants.map((t) => [t.id, t.name])), [tenants]);

  const rows = useMemo(() => {
    const q = search.trim().toLowerCase();
    return (rulesQuery.data ?? [])
      .filter((r) => tenantFilter === 'ALL' || r.tenantId === tenantFilter)
      .filter((r) => algoFilter === 'ALL' || r.algorithm === algoFilter)
      .filter((r) => !q || r.endpointPattern.toLowerCase().includes(q) || (tenantName.get(r.tenantId) ?? '').toLowerCase().includes(q))
      .sort((a, b) => (tenantName.get(a.tenantId) ?? '').localeCompare(tenantName.get(b.tenantId) ?? '') || a.endpointPattern.localeCompare(b.endpointPattern));
  }, [rulesQuery.data, tenantFilter, algoFilter, search, tenantName]);

  async function toggleActive(r: Rule) {
    setMenuFor(null);
    try {
      await setActive.mutateAsync({ id: r.id, active: !r.isActive });
      toast.success(`Rule ${r.isActive ? 'deactivated' : 'activated'}.`);
    } catch (e) {
      toast.error(toProblem(e).message);
    }
  }

  async function confirmDelete() {
    if (!toDelete) return;
    try {
      await remove.mutateAsync(toDelete.id);
      toast.success('Rule deleted.');
      setToDelete(null);
    } catch (e) {
      toast.error(toProblem(e).message);
    }
  }

  const filtersActive = search !== '' || tenantFilter !== 'ALL' || algoFilter !== 'ALL';
  const loading = rulesQuery.isLoading || tenantsQuery.isLoading;
  const error = rulesQuery.error ?? tenantsQuery.error;

  return (
    <>
      <PageHeader
        title="Rate limits"
        description="Rules override a tenant's tier default for requests matching a path pattern. The first matching active rule wins."
        actions={
          <Button variant="primary" onClick={() => setFormTarget('new')} disabled={tenants.length === 0}>
            <Plus className="h-4 w-4" aria-hidden /> New rule
          </Button>
        }
      />

      <Card className="mb-6">
        <CardHeader title="Tier defaults" description="Applied when no rule matches a request. Uses the token bucket algorithm." />
        <dl className="grid grid-cols-2 divide-x divide-slate-100 sm:grid-cols-4">
          {TIERS.map((t) => (
            <div key={t} className="px-5 py-3">
              <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">{t[0] + t.slice(1).toLowerCase()}</dt>
              <dd className="mt-1 text-sm font-semibold tabular-nums text-slate-900">{formatLimit(TIER_DEFAULTS[t].maxRequests, TIER_DEFAULTS[t].windowSeconds)}</dd>
            </div>
          ))}
        </dl>
      </Card>

      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 px-5 py-3">
          <div className="relative min-w-56 flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" aria-hidden />
            <Input aria-label="Search rules" placeholder="Search by path or tenant" value={search} onChange={(e) => setSearch(e.target.value)} className="pl-9" />
          </div>
          <Select aria-label="Filter by tenant" value={tenantFilter} onChange={(e) => setTenantFilter(e.target.value)} className="w-48">
            <option value="ALL">All tenants</option>
            {tenants.map((t) => (<option key={t.id} value={t.id}>{t.name}</option>))}
          </Select>
          <Select aria-label="Filter by algorithm" value={algoFilter} onChange={(e) => setAlgoFilter(e.target.value as 'ALL' | Algorithm)} className="w-52">
            <option value="ALL">All algorithms</option>
            {ALGORITHMS.map((a) => (<option key={a} value={a}>{ALGORITHM_INFO[a].label}</option>))}
          </Select>
        </div>

        {loading ? (
          <TableSkeleton cols={5} />
        ) : error ? (
          <ErrorState message={toProblem(error).message} onRetry={() => { rulesQuery.refetch(); tenantsQuery.refetch(); }} />
        ) : rows.length === 0 ? (
          filtersActive ? (
            <EmptyState icon={<Search className="h-5 w-5" />} title="No rules match your filters" description="Try a different search term or clear the filters."
              action={<Button onClick={() => { setSearch(''); setTenantFilter('ALL'); setAlgoFilter('ALL'); }}>Clear filters</Button>} />
          ) : tenants.length === 0 ? (
            <EmptyState icon={<SlidersHorizontal className="h-5 w-5" />} title="Create a tenant first" description="Rules belong to a tenant. Add a tenant on the Tenants page, then come back here." />
          ) : (
            <EmptyState icon={<SlidersHorizontal className="h-5 w-5" />} title="No custom rules yet" description="All tenants currently use their tier defaults. Add a rule to tighten or relax a specific endpoint."
              action={<Button variant="primary" onClick={() => setFormTarget('new')}><Plus className="h-4 w-4" aria-hidden /> New rule</Button>} />
          )
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[820px]">
              <thead className="border-b border-slate-200 bg-slate-50/60">
                <tr><Th>Path pattern</Th><Th>Tenant</Th><Th>Algorithm</Th><Th>Limit</Th><Th>Status</Th><Th className="w-12"><span className="sr-only">Actions</span></Th></tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {rows.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50/60">
                    <Td><code className="rounded bg-slate-100 px-1.5 py-0.5 font-mono text-xs text-slate-800">{r.endpointPattern}</code></Td>
                    <Td className="font-medium text-slate-900">{tenantName.get(r.tenantId) ?? <span className="text-slate-400">Unknown tenant</span>}</Td>
                    <Td><Badge tone="blue">{ALGORITHM_INFO[r.algorithm]?.label ?? r.algorithm}</Badge></Td>
                    <Td className="whitespace-nowrap tabular-nums">{formatLimit(r.maxRequests, r.windowSeconds)}</Td>
                    <Td><StatusDot active={r.isActive} /></Td>
                    <Td className="relative">
                      <button type="button" aria-label="Rule actions" aria-haspopup="menu" aria-expanded={menuFor === r.id}
                        onClick={() => setMenuFor(menuFor === r.id ? null : r.id)} className="rounded p-1.5 text-slate-500 hover:bg-slate-100">
                        <MoreHorizontal className="h-4 w-4" />
                      </button>
                      {menuFor === r.id && (
                        <>
                          <div className="fixed inset-0 z-10" onClick={() => setMenuFor(null)} aria-hidden />
                          <div role="menu" className="absolute right-4 top-10 z-20 w-44 rounded-md border border-slate-200 bg-white py-1 shadow-lg">
                            <button role="menuitem" className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-slate-700 hover:bg-slate-50" onClick={() => { setMenuFor(null); setFormTarget(r); }}>
                              <Pencil className="h-4 w-4" aria-hidden /> Edit
                            </button>
                            <button role="menuitem" className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-slate-700 hover:bg-slate-50" onClick={() => toggleActive(r)}>
                              <span className={`h-2 w-2 rounded-full ${r.isActive ? 'bg-slate-300' : 'bg-emerald-500'}`} aria-hidden />
                              {r.isActive ? 'Deactivate' : 'Activate'}
                            </button>
                            <button role="menuitem" className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-red-600 hover:bg-red-50" onClick={() => { setMenuFor(null); setToDelete(r); }}>
                              <Trash2 className="h-4 w-4" aria-hidden /> Delete
                            </button>
                          </div>
                        </>
                      )}
                    </Td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Modal open={formTarget !== null} onClose={() => setFormTarget(null)} title={formTarget && formTarget !== 'new' ? 'Edit rule' : 'New rule'} size="lg">
        {formTarget !== null && (
          <RuleForm
            key={formTarget === 'new' ? 'new' : formTarget.id}
            rule={formTarget === 'new' ? null : formTarget}
            tenants={tenants}
            onClose={() => setFormTarget(null)}
            onSaved={(edited) => { setFormTarget(null); toast.success(edited ? 'Rule updated.' : 'Rule created.'); }}
          />
        )}
      </Modal>

      <ConfirmDialog
        open={toDelete !== null}
        title="Delete rule"
        message={toDelete && <>Requests to <code className="font-mono text-xs">{toDelete.endpointPattern}</code> for {tenantName.get(toDelete.tenantId) ?? 'this tenant'} will fall back to the tier default.</>}
        confirmLabel="Delete rule"
        loading={remove.isPending}
        onConfirm={confirmDelete}
        onClose={() => setToDelete(null)}
      />
    </>
  );
}

function RuleForm({ rule, tenants, onClose, onSaved }: {
  rule: Rule | null;
  tenants: Tenant[];
  onClose: () => void;
  onSaved: (edited: boolean) => void;
}) {
  const create = useCreateRule();
  const update = useUpdateRule();
  const [tenantId, setTenantId] = useState(rule?.tenantId ?? tenants[0]?.id ?? '');
  const [pattern, setPattern] = useState(rule?.endpointPattern ?? '');
  const [algorithm, setAlgorithm] = useState<Algorithm>(rule?.algorithm ?? 'TOKEN_BUCKET');
  const [maxRequests, setMaxRequests] = useState(String(rule?.maxRequests ?? 100));
  const [windowSeconds, setWindowSeconds] = useState(String(rule?.windowSeconds ?? 60));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const pending = create.isPending || update.isPending;

  const perMinute = Number(maxRequests) > 0 && Number(windowSeconds) > 0
    ? Math.round((Number(maxRequests) / Number(windowSeconds)) * 60) : null;

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    const local: Record<string, string> = {};
    if (!tenantId) local.tenantId = 'Choose a tenant';
    if (!pattern.trim()) local.endpointPattern = 'Path pattern is required';
    else if (!pattern.trim().startsWith('/')) local.endpointPattern = 'Pattern must start with /';
    if (!(Number.isInteger(Number(maxRequests)) && Number(maxRequests) >= 1)) local.maxRequests = 'Enter a whole number of at least 1';
    if (!(Number.isInteger(Number(windowSeconds)) && Number(windowSeconds) >= 1)) local.windowSeconds = 'Enter a whole number of at least 1';
    setErrors(local);
    setFormError(null);
    if (Object.keys(local).length) return;

    const body = { tenantId, endpointPattern: pattern.trim(), algorithm, maxRequests: Number(maxRequests), windowSeconds: Number(windowSeconds) };
    try {
      if (rule) await update.mutateAsync({ id: rule.id, body });
      else await create.mutateAsync(body);
      onSaved(!!rule);
    } catch (err) {
      const problem = toProblem(err);
      setErrors(problem.fieldErrors);
      setFormError(problem.message);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="space-y-4">
      <Field label="Tenant" htmlFor="rule-tenant" error={errors.tenantId}>
        <Select id="rule-tenant" value={tenantId} onChange={(e) => setTenantId(e.target.value)} disabled={!!rule} error={errors.tenantId}>
          {tenants.map((t) => (<option key={t.id} value={t.id}>{t.name}</option>))}
        </Select>
      </Field>
      <Field label="Path pattern" htmlFor="rule-pattern" error={errors.endpointPattern}
        hint="Use * for one path segment and ** for any depth, e.g. /api/v1/orders/**">
        <Input id="rule-pattern" value={pattern} onChange={(e) => setPattern(e.target.value)} error={errors.endpointPattern} placeholder="/api/v1/orders/**" className="font-mono" autoFocus />
      </Field>
      <Field label="Algorithm" htmlFor="rule-algorithm" error={errors.algorithm} hint={ALGORITHM_INFO[algorithm].description}>
        <Select id="rule-algorithm" value={algorithm} onChange={(e) => setAlgorithm(e.target.value as Algorithm)}>
          {ALGORITHMS.map((a) => (<option key={a} value={a}>{ALGORITHM_INFO[a].label}</option>))}
        </Select>
      </Field>
      <div className="grid grid-cols-2 gap-4">
        <Field label="Max requests" htmlFor="rule-max" error={errors.maxRequests}>
          <Input id="rule-max" type="number" min={1} step={1} value={maxRequests} onChange={(e) => setMaxRequests(e.target.value)} error={errors.maxRequests} />
        </Field>
        <Field label="Window (seconds)" htmlFor="rule-window" error={errors.windowSeconds}>
          <Input id="rule-window" type="number" min={1} step={1} value={windowSeconds} onChange={(e) => setWindowSeconds(e.target.value)} error={errors.windowSeconds} />
        </Field>
      </div>
      {perMinute !== null && <p className="text-xs text-slate-500">That is about {perMinute.toLocaleString()} requests per minute.</p>}
      {formError && !Object.keys(errors).length && <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{formError}</p>}
      <div className="flex justify-end gap-2 pt-2">
        <Button type="button" onClick={onClose} disabled={pending}>Cancel</Button>
        <Button type="submit" variant="primary" loading={pending}>{rule ? 'Save changes' : 'Create rule'}</Button>
      </div>
    </form>
  );
}
