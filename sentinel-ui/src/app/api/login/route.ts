import { NextResponse } from 'next/server';
import { SESSION_COOKIE, SESSION_TTL_SECONDS, createSession, safeEqual, sessionSecret } from '@/lib/session';

export async function POST(request: Request) {
  const secret = sessionSecret();
  const expectedUser = process.env.UI_ADMIN_USER;
  const expectedPassword = process.env.UI_ADMIN_PASSWORD;
  if (!secret || !expectedUser || !expectedPassword) {
    // Fail closed: never allow a login when the server is not configured
    return NextResponse.json({ error: 'login is not configured' }, { status: 503 });
  }

  let body: { username?: unknown; password?: unknown };
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: 'invalid request' }, { status: 400 });
  }
  const username = typeof body.username === 'string' ? body.username : '';
  const password = typeof body.password === 'string' ? body.password : '';

  const userOk = await safeEqual(username, expectedUser);
  const passwordOk = await safeEqual(password, expectedPassword);
  if (!userOk || !passwordOk) {
    return NextResponse.json({ error: 'invalid credentials' }, { status: 401 });
  }

  const response = NextResponse.json({ ok: true });
  response.cookies.set(SESSION_COOKIE, await createSession(username, secret), {
    httpOnly: true,
    sameSite: 'strict',
    secure: process.env.NODE_ENV === 'production' && process.env.INSECURE_COOKIES !== 'true',
    path: '/',
    maxAge: SESSION_TTL_SECONDS,
  });
  return response;
}
