'use client';

import { useMemo, useState } from 'react';
import { Eye, EyeOff, MoreHorizontal, Pencil, Plus, Search, Trash2, Users } from 'lucide-react';
import {
  Badge, Button, Card, ConfirmDialog, CopyButton, EmptyState, ErrorState, Field, Input, Modal, PageHeader,
  Select, StatusDot, TableSkeleton, Td, Th,
} from '@/components/ui';
import { useToast } from '@/components/toast';
import { toProblem } from '@/lib/api';
import { formatDate, formatLimit, maskKey } from '@/lib/format';
import {
  useCreateTenant, useDeleteTenant, useSetTenantActive, useTenants, useUpdateTenant,
} from '@/lib/queries';
import { TIERS, TIER_DEFAULTS } from '@/types';
import type { Tenant, Tier } from '@/types';

const TIER_TONE: Record<Tier, 'neutral' | 'blue' | 'amber' | 'green'> = {
  FREE: 'neutral', BASIC: 'blue', PREMIUM: 'amber', ENTERPRISE: 'green',
};

export default function TenantsPage() {
  const toast = useToast();
  const { data, isLoading, error, refetch } = useTenants();
  const setActive = useSetTenantActive();
  const remove = useDeleteTenant();

  const [search, setSearch] = useState('');
  const [tier, setTier] = useState<'ALL' | Tier>('ALL');
  const [status, setStatus] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  const [formTarget, setFormTarget] = useState<Tenant | 'new' | null>(null);
  const [created, setCreated] = useState<Tenant | null>(null);
  const [toDelete, setToDelete] = useState<Tenant | null>(null);
  const [revealed, setRevealed] = useState<Set<string>>(new Set());
  const [menuFor, setMenuFor] = useState<string | null>(null);

  const rows = useMemo(() => {
    const q = search.trim().toLowerCase();
    return (data ?? [])
      .filter((t) => tier === 'ALL' || t.tier === tier)
      .filter((t) => status === 'ALL' || (status === 'ACTIVE') === t.isActive)
      .filter((t) => !q || t.name.toLowerCase().includes(q) || (t.contactEmail ?? '').toLowerCase().includes(q))
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [data, search, tier, status]);

  const toggleReveal = (id: string) =>
    setRevealed((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  async function toggleActive(t: Tenant) {
    setMenuFor(null);
    try {
      await setActive.mutateAsync({ id: t.id, active: !t.isActive });
      toast.success(`${t.name} ${t.isActive ? 'deactivated' : 'activated'}.`);
    } catch (e) {
      toast.error(toProblem(e).message);
    }
  }

  async function confirmDelete() {
    if (!toDelete) return;
    try {
      await remove.mutateAsync(toDelete.id);
      toast.success(`${toDelete.name} deleted.`);
      setToDelete(null);
    } catch (e) {
      toast.error(toProblem(e).message);
    }
  }

  const filtersActive = search !== '' || tier !== 'ALL' || status !== 'ALL';

  return (
    <>
      <PageHeader
        title="Tenants"
        description="Customers that call your APIs through the gateway. A tenant's tier sets its default rate limit."
        actions={
          <Button variant="primary" onClick={() => setFormTarget('new')}>
            <Plus className="h-4 w-4" aria-hidden /> New tenant
          </Button>
        }
      />

      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 px-5 py-3">
          <div className="relative min-w-56 flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" aria-hidden />
            <Input
              aria-label="Search tenants"
              placeholder="Search by name or email"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9"
            />
          </div>
          <Select aria-label="Filter by tier" value={tier} onChange={(e) => setTier(e.target.value as 'ALL' | Tier)} className="w-40">
            <option value="ALL">All tiers</option>
            {TIERS.map((t) => (<option key={t} value={t}>{t[0] + t.slice(1).toLowerCase()}</option>))}
          </Select>
          <Select aria-label="Filter by status" value={status} onChange={(e) => setStatus(e.target.value as 'ALL' | 'ACTIVE' | 'INACTIVE')} className="w-40">
            <option value="ALL">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </Select>
        </div>

        {isLoading ? (
          <TableSkeleton cols={5} />
        ) : error ? (
          <ErrorState message={toProblem(error).message} onRetry={() => refetch()} />
        ) : rows.length === 0 ? (
          filtersActive ? (
            <EmptyState
              icon={<Search className="h-5 w-5" />}
              title="No tenants match your filters"
              description="Try a different search term or clear the filters."
              action={<Button onClick={() => { setSearch(''); setTier('ALL'); setStatus('ALL'); }}>Clear filters</Button>}
            />
          ) : (
            <EmptyState
              icon={<Users className="h-5 w-5" />}
              title="No tenants yet"
              description="Create your first tenant to get an API key and start applying rate limits."
              action={<Button variant="primary" onClick={() => setFormTarget('new')}><Plus className="h-4 w-4" aria-hidden /> New tenant</Button>}
            />
          )
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px]">
              <thead className="border-b border-slate-200 bg-slate-50/60">
                <tr>
                  <Th>Tenant</Th><Th>Tier</Th><Th>Status</Th><Th>API key</Th><Th>Created</Th><Th className="w-12"><span className="sr-only">Actions</span></Th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {rows.map((t) => (
                  <tr key={t.id} className="hover:bg-slate-50/60">
                    <Td>
                      <div className="font-medium text-slate-900">{t.name}</div>
                      <div className="text-xs text-slate-500">{t.contactEmail || 'No contact email'}</div>
                    </Td>
                    <Td className="whitespace-nowrap">
                      <Badge tone={TIER_TONE[t.tier]}>{t.tier[0] + t.tier.slice(1).toLowerCase()}</Badge>
                      <div className="mt-0.5 text-xs text-slate-500">{formatLimit(TIER_DEFAULTS[t.tier].maxRequests, TIER_DEFAULTS[t.tier].windowSeconds)}</div>
                    </Td>
                    <Td><StatusDot active={t.isActive} /></Td>
                    <Td>
                      <div className="flex items-center gap-1">
                        <code className="rounded bg-slate-100 px-1.5 py-0.5 font-mono text-xs text-slate-700">
                          {revealed.has(t.id) ? t.apiKey : maskKey(t.apiKey)}
                        </code>
                        <button
                          type="button"
                          onClick={() => toggleReveal(t.id)}
                          aria-label={revealed.has(t.id) ? 'Hide API key' : 'Show API key'}
                          className="rounded p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-700"
                        >
                          {revealed.has(t.id) ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                        </button>
                        <CopyButton text={t.apiKey} label="Copy API key" />
                      </div>
                    </Td>
                    <Td className="whitespace-nowrap text-slate-500">{formatDate(t.createdAt)}</Td>
                    <Td className="relative">
                      <button
                        type="button"
                        aria-label={`Actions for ${t.name}`}
                        aria-haspopup="menu"
                        aria-expanded={menuFor === t.id}
                        onClick={() => setMenuFor(menuFor === t.id ? null : t.id)}
                        className="rounded p-1.5 text-slate-500 hover:bg-slate-100"
                      >
                        <MoreHorizontal className="h-4 w-4" />
                      </button>
                      {menuFor === t.id && (
                        <>
                          <div className="fixed inset-0 z-10" onClick={() => setMenuFor(null)} aria-hidden />
                          <div role="menu" className="absolute right-4 top-10 z-20 w-44 rounded-md border border-slate-200 bg-white py-1 shadow-lg">
                            <button role="menuitem" className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-slate-700 hover:bg-slate-50" onClick={() => { setMenuFor(null); setFormTarget(t); }}>
                              <Pencil className="h-4 w-4" aria-hidden /> Edit
                            </button>
                            <button role="menuitem" className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-slate-700 hover:bg-slate-50" onClick={() => toggleActive(t)}>
                              <span className={`h-2 w-2 rounded-full ${t.isActive ? 'bg-slate-300' : 'bg-emerald-500'}`} aria-hidden />
                              {t.isActive ? 'Deactivate' : 'Activate'}
                            </button>
                            <button role="menuitem" className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-red-600 hover:bg-red-50" onClick={() => { setMenuFor(null); setToDelete(t); }}>
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

      <TenantFormModal
        target={formTarget}
        onClose={() => setFormTarget(null)}
        onCreated={(t) => { setFormTarget(null); setCreated(t); }}
        onUpdated={(t) => { setFormTarget(null); toast.success(`${t.name} updated.`); }}
      />

      <Modal
        open={created !== null}
        onClose={() => setCreated(null)}
        title="Tenant created"
        description="Share this API key with the customer. They exchange it for a token at POST /auth/token."
        footer={<Button variant="primary" onClick={() => setCreated(null)}>Done</Button>}
      >
        {created && (
          <div className="space-y-3">
            <p className="text-sm text-slate-700"><span className="font-medium">{created.name}</span> is on the {created.tier.toLowerCase()} tier.</p>
            <div className="flex items-center justify-between gap-2 rounded-md border border-slate-200 bg-slate-50 px-3 py-2">
              <code className="break-all font-mono text-xs text-slate-800">{created.apiKey}</code>
              <CopyButton text={created.apiKey} label="Copy API key" />
            </div>
          </div>
        )}
      </Modal>

      <ConfirmDialog
        open={toDelete !== null}
        title="Delete tenant"
        message={toDelete && <>This permanently deletes <strong>{toDelete.name}</strong> and its API key. Requests using that key will stop working.</>}
        confirmLabel="Delete tenant"
        loading={remove.isPending}
        onConfirm={confirmDelete}
        onClose={() => setToDelete(null)}
      />
    </>
  );
}

function TenantFormModal({
  target, onClose, onCreated, onUpdated,
}: {
  target: Tenant | 'new' | null;
  onClose: () => void;
  onCreated: (t: Tenant) => void;
  onUpdated: (t: Tenant) => void;
}) {
  const open = target !== null;
  const editing = target !== null && target !== 'new' ? target : null;
  // Remount on each open so the form always starts from the right values
  return (
    <Modal open={open} onClose={onClose} title={editing ? 'Edit tenant' : 'New tenant'}>
      {open && <TenantForm key={editing?.id ?? 'new'} tenant={editing} onClose={onClose} onCreated={onCreated} onUpdated={onUpdated} />}
    </Modal>
  );
}

function TenantForm({
  tenant, onClose, onCreated, onUpdated,
}: {
  tenant: Tenant | null;
  onClose: () => void;
  onCreated: (t: Tenant) => void;
  onUpdated: (t: Tenant) => void;
}) {
  const create = useCreateTenant();
  const update = useUpdateTenant();
  const [name, setName] = useState(tenant?.name ?? '');
  const [tier, setTier] = useState<Tier>(tenant?.tier ?? 'FREE');
  const [email, setEmail] = useState(tenant?.contactEmail ?? '');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const pending = create.isPending || update.isPending;

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    const local: Record<string, string> = {};
    if (!name.trim()) local.name = 'Name is required';
    if (email && !/^\S+@\S+\.\S+$/.test(email)) local.contactEmail = 'Enter a valid email address';
    setErrors(local);
    setFormError(null);
    if (Object.keys(local).length) return;

    const body = { name: name.trim(), tier, contactEmail: email.trim() || undefined };
    try {
      if (tenant) onUpdated(await update.mutateAsync({ id: tenant.id, body }));
      else onCreated(await create.mutateAsync(body));
    } catch (err) {
      const problem = toProblem(err);
      setErrors(problem.fieldErrors);
      setFormError(problem.message);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="space-y-4">
      <Field label="Name" htmlFor="tenant-name" error={errors.name}>
        <Input id="tenant-name" value={name} onChange={(e) => setName(e.target.value)} error={errors.name} autoFocus placeholder="Acme Corp" />
      </Field>
      <Field label="Tier" htmlFor="tenant-tier" error={errors.tier}
        hint={`Default limit: ${formatLimit(TIER_DEFAULTS[tier].maxRequests, TIER_DEFAULTS[tier].windowSeconds)}`}>
        <Select id="tenant-tier" value={tier} onChange={(e) => setTier(e.target.value as Tier)}>
          {TIERS.map((t) => (<option key={t} value={t}>{t[0] + t.slice(1).toLowerCase()}</option>))}
        </Select>
      </Field>
      <Field label="Contact email" htmlFor="tenant-email" error={errors.contactEmail} hint="Optional">
        <Input id="tenant-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} error={errors.contactEmail} placeholder="ops@acme.com" />
      </Field>
      {formError && !Object.keys(errors).length && <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{formError}</p>}
      <div className="flex justify-end gap-2 pt-2">
        <Button type="button" onClick={onClose} disabled={pending}>Cancel</Button>
        <Button type="submit" variant="primary" loading={pending}>{tenant ? 'Save changes' : 'Create tenant'}</Button>
      </div>
    </form>
  );
}
