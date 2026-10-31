import type { NextRequest } from 'next/server';

const ADMIN_API_URL = process.env.ADMIN_API_URL || 'http://localhost:8081';
const ADMIN_API_KEY = process.env.ADMIN_API_KEY || '';

async function proxy(request: NextRequest, ctx: { params: Promise<{ path: string[] }> }) {
  const { path } = await ctx.params;
  const target = `${ADMIN_API_URL}/api/v1/admin/${path.map(encodeURIComponent).join('/')}${request.nextUrl.search}`;

  const headers = new Headers();
  const contentType = request.headers.get('content-type');
  if (contentType) headers.set('content-type', contentType);
  headers.set('X-Admin-Key', ADMIN_API_KEY);

  const hasBody = request.method !== 'GET' && request.method !== 'HEAD';
  let upstream: Response;
  try {
    upstream = await fetch(target, {
      method: request.method,
      headers,
      body: hasBody ? await request.text() : undefined,
      cache: 'no-store',
      signal: AbortSignal.timeout(10_000),
    });
  } catch {
    return Response.json({ error: 'admin service unavailable' }, { status: 502 });
  }

  return new Response(upstream.status === 204 ? null : await upstream.text(), {
    status: upstream.status,
    headers: { 'content-type': upstream.headers.get('content-type') ?? 'application/json' },
  });
}

export { proxy as GET, proxy as POST, proxy as PUT, proxy as DELETE, proxy as PATCH };
