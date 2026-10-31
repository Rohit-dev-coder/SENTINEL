// Shapes mirror the admin service's JSON exactly (UUID ids, `isActive`, `endpointPattern`).

export const TIERS = ['FREE', 'BASIC', 'PREMIUM', 'ENTERPRISE'] as const;
export type Tier = (typeof TIERS)[number];

/** Default limits per tier; must match sentinel-common Tier.java. */
export const TIER_DEFAULTS: Record<Tier, { maxRequests: number; windowSeconds: number }> = {
  FREE: { maxRequests: 10, windowSeconds: 60 },
  BASIC: { maxRequests: 100, windowSeconds: 60 },
  PREMIUM: { maxRequests: 1000, windowSeconds: 60 },
  ENTERPRISE: { maxRequests: 10000, windowSeconds: 60 },
};

export const ALGORITHMS = [
  'TOKEN_BUCKET',
  'SLIDING_WINDOW_LOG',
  'FIXED_WINDOW_COUNTER',
  'SLIDING_WINDOW_COUNTER',
] as const;
export type Algorithm = (typeof ALGORITHMS)[number];

export const ALGORITHM_INFO: Record<Algorithm, { label: string; description: string }> = {
  TOKEN_BUCKET: { label: 'Token bucket', description: 'Allows short bursts, then a steady rate.' },
  SLIDING_WINDOW_LOG: { label: 'Sliding window log', description: 'Exact count of the last N seconds; uses more memory.' },
  FIXED_WINDOW_COUNTER: { label: 'Fixed window counter', description: 'Simple count per calendar window; can double at boundaries.' },
  SLIDING_WINDOW_COUNTER: { label: 'Sliding window counter', description: 'Near-exact limit using 10-second slices.' },
};

export interface Tenant {
  id: string;
  name: string;
  apiKey: string;
  tier: Tier;
  isActive: boolean;
  contactEmail: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TenantRequest {
  name: string;
  tier: Tier;
  contactEmail?: string;
}

export interface Rule {
  id: string;
  tenantId: string;
  endpointPattern: string;
  algorithm: Algorithm;
  maxRequests: number;
  windowSeconds: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface RuleRequest {
  tenantId: string;
  endpointPattern: string;
  algorithm: Algorithm;
  maxRequests: number;
  windowSeconds: number;
}

export type ServiceState = 'up' | 'down' | 'unreachable';

export interface SystemStatus {
  services: { id: string; name: string; state: ServiceState; latencyMs: number | null }[];
  metrics: { available: boolean; requestsPerMinute: number | null; throttledPerMinute: number | null; errorsPerMinute: number | null };
  checkedAt: string;
}
