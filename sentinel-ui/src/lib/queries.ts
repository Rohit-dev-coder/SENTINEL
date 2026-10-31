import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { Rule, RuleRequest, SystemStatus, Tenant, TenantRequest } from '@/types';

const get = async <T,>(url: string) => (await api.get<T>(url)).data;

export const useTenants = () => useQuery({ queryKey: ['tenants'], queryFn: () => get<Tenant[]>('/tenants') });
export const useRules = () => useQuery({ queryKey: ['rules'], queryFn: () => get<Rule[]>('/rules') });

export const useSystemStatus = () =>
  useQuery({
    queryKey: ['system-status'],
    queryFn: async () => (await fetch('/api/system/status', { cache: 'no-store' }).then((r) => r.json())) as SystemStatus,
    refetchInterval: 15000,
  });

function useInvalidating<TVars, TData = unknown>(key: 'tenants' | 'rules', fn: (v: TVars) => Promise<TData>) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: () => client.invalidateQueries({ queryKey: [key] }),
  });
}

export const useCreateTenant = () =>
  useInvalidating('tenants', async (body: TenantRequest) => (await api.post<Tenant>('/tenants', body)).data);
export const useUpdateTenant = () =>
  useInvalidating('tenants', async ({ id, body }: { id: string; body: TenantRequest }) =>
    (await api.put<Tenant>(`/tenants/${id}`, body)).data);
export const useDeleteTenant = () =>
  useInvalidating('tenants', async (id: string) => { await api.delete(`/tenants/${id}`); });
export const useSetTenantActive = () =>
  useInvalidating('tenants', async ({ id, active }: { id: string; active: boolean }) => {
    await api.post(`/tenants/${id}/${active ? 'activate' : 'deactivate'}`);
  });

export const useCreateRule = () =>
  useInvalidating('rules', async (body: RuleRequest) => (await api.post<Rule>('/rules', body)).data);
export const useUpdateRule = () =>
  useInvalidating('rules', async ({ id, body }: { id: string; body: RuleRequest }) =>
    (await api.put<Rule>(`/rules/${id}`, body)).data);
export const useDeleteRule = () =>
  useInvalidating('rules', async (id: string) => { await api.delete(`/rules/${id}`); });
export const useSetRuleActive = () =>
  useInvalidating('rules', async ({ id, active }: { id: string; active: boolean }) => {
    await api.post(`/rules/${id}/${active ? 'activate' : 'deactivate'}`);
  });
