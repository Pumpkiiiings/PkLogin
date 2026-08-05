'use client';

/**
 * Silences one false-positive React warning in development.
 *
 * `next-themes` injects its no-flash script with `createElement('script', …)`.
 * React 19 warns about any `<script>` rendered inside a component, because one
 * created during a client render would never execute — but this one is served
 * in the SSR stream and does run, so the warning is wrong here.
 *
 * next-themes has been unmaintained since March 2025 and `fumadocs-ui` depends
 * on it, so there is nothing to upgrade to. Next 16.2 forwards browser console
 * errors to the terminal and flags them in the dev overlay, which makes a
 * warning we cannot act on cost attention on every page load.
 *
 * Filtered by exact message so real errors still get through.
 *
 * @see https://github.com/pacocoursey/next-themes/issues/387
 */
const SUPPRESSED = 'Encountered a script tag while rendering React component';

if (process.env.NODE_ENV === 'development' && typeof window !== 'undefined') {
  const original = console.error;

  console.error = (...args: unknown[]) => {
    if (typeof args[0] === 'string' && args[0].includes(SUPPRESSED)) return;
    original(...args);
  };
}

export function DevWarnings() {
  return null;
}
