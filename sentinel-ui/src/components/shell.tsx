'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useState } from 'react';
import type { ReactNode } from 'react';
import { Activity, LayoutDashboard, LogOut, Menu, ShieldCheck, SlidersHorizontal, Users, X } from 'lucide-react';
import { cx } from '@/components/ui';

const NAV = [
  { href: '/dashboard', label: 'Overview', icon: LayoutDashboard, exact: true },
  { href: '/dashboard/tenants', label: 'Tenants', icon: Users },
  { href: '/dashboard/rate-limits', label: 'Rate limits', icon: SlidersHorizontal },
  { href: '/dashboard/monitoring', label: 'Monitoring', icon: Activity },
];

export default function Shell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [open, setOpen] = useState(false);

  async function signOut() {
    await fetch('/api/logout', { method: 'POST' });
    router.push('/');
  }

  const nav = (
    <nav aria-label="Main" className="flex-1 space-y-1 px-3 py-4">
      {NAV.map(({ href, label, icon: Icon, exact }) => {
        const active = exact ? pathname === href : pathname.startsWith(href);
        return (
          <Link
            key={href}
            href={href}
            onClick={() => setOpen(false)}
            aria-current={active ? 'page' : undefined}
            className={cx(
              'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
              active ? 'bg-slate-800 text-white' : 'text-slate-300 hover:bg-slate-800/60 hover:text-white'
            )}
          >
            <Icon className="h-4 w-4" aria-hidden />
            {label}
          </Link>
        );
      })}
    </nav>
  );

  const brand = (
    <div className="flex h-14 items-center gap-2.5 px-5">
      <ShieldCheck className="h-5 w-5 text-blue-400" aria-hidden />
      <span className="text-sm font-semibold tracking-wide text-white">SENTINEL</span>
    </div>
  );

  const footer = (
    <div className="border-t border-slate-800 p-3">
      <button
        onClick={signOut}
        className="flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-slate-300 hover:bg-slate-800/60 hover:text-white"
      >
        <LogOut className="h-4 w-4" aria-hidden />
        Sign out
      </button>
    </div>
  );

  return (
    <div className="min-h-screen">
      {/* Desktop sidebar */}
      <aside className="fixed inset-y-0 left-0 hidden w-60 flex-col bg-slate-900 lg:flex">
        {brand}
        {nav}
        {footer}
      </aside>

      {/* Mobile drawer */}
      {open && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-slate-900/50" onClick={() => setOpen(false)} aria-hidden />
          <aside className="absolute inset-y-0 left-0 flex w-64 flex-col bg-slate-900">
            <div className="flex items-center justify-between pr-3">
              {brand}
              <button onClick={() => setOpen(false)} aria-label="Close menu" className="rounded p-1 text-slate-300 hover:bg-slate-800">
                <X className="h-5 w-5" />
              </button>
            </div>
            {nav}
            {footer}
          </aside>
        </div>
      )}

      <div className="lg:pl-60">
        <header className="sticky top-0 z-30 flex h-14 items-center gap-3 border-b border-slate-200 bg-white/90 px-4 backdrop-blur lg:hidden">
          <button onClick={() => setOpen(true)} aria-label="Open menu" className="rounded p-1.5 text-slate-600 hover:bg-slate-100">
            <Menu className="h-5 w-5" />
          </button>
          <span className="text-sm font-semibold text-slate-900">SENTINEL</span>
        </header>
        <main className="mx-auto max-w-6xl px-4 py-8 sm:px-8">{children}</main>
      </div>
    </div>
  );
}
