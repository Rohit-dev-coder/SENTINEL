import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { SESSION_COOKIE, sessionSecret, verifySession } from '@/lib/session';

// Guards the dashboard pages and the admin API proxy. Everything else (login page, login/logout
// endpoints, static assets) stays public.
export async function proxy(request: NextRequest) {
  const valid = await verifySession(request.cookies.get(SESSION_COOKIE)?.value, sessionSecret());
  if (valid) {
    return NextResponse.next();
  }
  if (request.nextUrl.pathname.startsWith('/api/')) {
    return NextResponse.json({ error: 'unauthorized' }, { status: 401 });
  }
  return NextResponse.redirect(new URL('/', request.url));
}

export const config = {
  matcher: ['/dashboard/:path*', '/api/admin/:path*', '/api/system/:path*'],
};
