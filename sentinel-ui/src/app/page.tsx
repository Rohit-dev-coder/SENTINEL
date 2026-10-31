'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { ShieldCheck } from 'lucide-react';
import { Button, Field, Input } from '@/components/ui';

export default function SignIn() {
  const router = useRouter();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const response = await fetch('/api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });
      if (response.ok) {
        router.push('/dashboard');
        return;
      }
      setError(
        response.status === 503
          ? 'Sign-in is not configured on the server. Ask an administrator to set the UI credentials.'
          : 'Incorrect username or password.'
      );
    } catch {
      setError('Could not reach the server. Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex flex-col items-center text-center">
          <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-lg bg-slate-900">
            <ShieldCheck className="h-6 w-6 text-blue-400" aria-hidden />
          </div>
          <h1 className="text-xl font-semibold tracking-tight text-slate-900">Sign in to SENTINEL</h1>
          <p className="mt-1 text-sm text-slate-500">API gateway and rate limiter console</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4 rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <Field label="Username" htmlFor="username">
            <Input
              id="username"
              autoComplete="username"
              required
              autoFocus
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </Field>
          <Field label="Password" htmlFor="password">
            <Input
              id="password"
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </Field>

          {error && (
            <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </p>
          )}

          <Button type="submit" variant="primary" loading={submitting} className="w-full">
            Sign in
          </Button>
        </form>
      </div>
    </div>
  );
}
