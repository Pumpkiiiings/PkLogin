export const appName = 'PkLogin';
export const appDescription =
  'Authentication for Spigot, Paper, Folia and Velocity — passwordless premium login, 2FA, five database engines and 18 translations.';
export const docsRoute = '/docs';
export const docsImageRoute = '/og/docs';
export const docsContentRoute = '/llms.mdx/docs';

export const gitConfig = {
  user: 'Pumpkiiiings',
  repo: 'PkLogin',
  branch: 'main',
};

export const links = {
  releases: `https://github.com/${gitConfig.user}/${gitConfig.repo}/releases`,
  issues: `https://github.com/${gitConfig.user}/${gitConfig.repo}/issues`,
  website: 'https://www.pumpkiiings.com',
  discord: 'https://www.pumpkiiings.com/discord',
  maven: 'https://repo.pumpkiiings.com/maven-releases/',
};

/** Version of the plugin these docs describe, from the root `build.gradle`. */
export const pluginVersion = '2.1';

/**
 * Absolute base for OG image and canonical URLs. Vercel injects
 * `VERCEL_PROJECT_PRODUCTION_URL` on every deployment; set `NEXT_PUBLIC_SITE_URL`
 * to override it with your own domain.
 */
export function siteUrl(): URL {
  const explicit = process.env.NEXT_PUBLIC_SITE_URL;
  if (explicit) return new URL(explicit);

  const vercel = process.env.VERCEL_PROJECT_PRODUCTION_URL;
  if (vercel) return new URL(`https://${vercel}`);

  return new URL('http://localhost:3000');
}
