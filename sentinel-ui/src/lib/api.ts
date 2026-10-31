import axios, { AxiosError } from 'axios';

// Same-origin proxy (src/app/api/admin) forwards to the admin service and adds the admin key
const api = axios.create({
  baseURL: '/api/admin',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401 && typeof window !== 'undefined') {
      // Session expired or missing: full reload back to the sign-in page
      // eslint-disable-next-line @next/next/no-location-assign-relative-destination -- full reload clears client state
      window.location.href = '/';
    }
    return Promise.reject(error);
  }
);

export interface ApiProblem {
  message: string;
  fieldErrors: Record<string, string>;
}

/** Turns an axios/network error into a friendly message plus per-field errors from the admin API. */
export function toProblem(error: unknown): ApiProblem {
  const fallback: ApiProblem = { message: 'Something went wrong. Please try again.', fieldErrors: {} };
  if (!axios.isAxiosError(error)) return fallback;
  if (!error.response) return { message: 'Cannot reach the server. Check your connection.', fieldErrors: {} };
  const data = error.response.data as { detail?: string; error?: string; errors?: Record<string, string> } | undefined;
  const status = error.response.status;
  if (status === 502) return { message: 'The admin service is unavailable right now.', fieldErrors: {} };
  if (status === 404) return { message: data?.detail ?? 'That item no longer exists.', fieldErrors: {} };
  if (status === 400 && data?.errors) return { message: 'Please fix the highlighted fields.', fieldErrors: data.errors };
  return { message: data?.detail ?? data?.error ?? fallback.message, fieldErrors: {} };
}

export default api;
