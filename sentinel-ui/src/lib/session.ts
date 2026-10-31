// Signed, expiring session cookie using Web Crypto so it works in both the proxy and route handlers.

export const SESSION_COOKIE = 'sentinel_session';
export const SESSION_TTL_SECONDS = 8 * 60 * 60;

const encoder = new TextEncoder();

function toHex(buffer: ArrayBuffer): string {
  return Array.from(new Uint8Array(buffer))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

async function hmac(value: string, secret: string): Promise<string> {
  const key = await crypto.subtle.importKey(
    'raw',
    encoder.encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  );
  return toHex(await crypto.subtle.sign('HMAC', key, encoder.encode(value)));
}

/** Compares two strings without leaking where they differ. */
export async function safeEqual(a: string, b: string): Promise<boolean> {
  const [ha, hb] = await Promise.all([
    crypto.subtle.digest('SHA-256', encoder.encode(a)),
    crypto.subtle.digest('SHA-256', encoder.encode(b)),
  ]);
  const x = new Uint8Array(ha);
  const y = new Uint8Array(hb);
  let diff = 0;
  for (let i = 0; i < x.length; i++) diff |= x[i] ^ y[i];
  return diff === 0;
}

export function sessionSecret(): string | null {
  const secret = process.env.SESSION_SECRET;
  return secret && secret.length >= 32 ? secret : null;
}

export async function createSession(user: string, secret: string): Promise<string> {
  const expires = Math.floor(Date.now() / 1000) + SESSION_TTL_SECONDS;
  const payload = `${encodeURIComponent(user)}.${expires}`;
  return `${payload}.${await hmac(payload, secret)}`;
}

export async function verifySession(token: string | undefined, secret: string | null): Promise<boolean> {
  if (!token || !secret) return false;
  const lastDot = token.lastIndexOf('.');
  if (lastDot < 0) return false;
  const payload = token.slice(0, lastDot);
  const signature = token.slice(lastDot + 1);
  const expires = Number(payload.slice(payload.lastIndexOf('.') + 1));
  if (!Number.isFinite(expires) || expires < Math.floor(Date.now() / 1000)) return false;
  return safeEqual(signature, await hmac(payload, secret));
}
