# pklogin-docs

Documentation site for [PkLogin](https://github.com/Pumpkiiiings/PkLogin), built with
[Next.js](https://nextjs.org) and [Fumadocs](https://fumadocs.dev).

Bilingual (English and Spanish), deployed on Vercel.

## Development

```bash
npm install
npm run dev
```

Open <http://localhost:3000>. `/` redirects to `/en` or `/es` depending on your browser's
`Accept-Language`.

```bash
npm run build         # production build
npm run types:check   # next typegen + tsc --noEmit
npm run lint          # eslint
```

## Layout

| Path | What it is |
| --- | --- |
| `content/docs/en` | English pages. |
| `content/docs/es` | Spanish pages. |
| `src/lib/i18n.ts` | Language list, URL strategy and the Spanish UI strings. |
| `src/lib/source.ts` | Content source adapter — [`loader()`](https://fumadocs.dev/docs/headless/source-api). |
| `src/lib/shared.ts` | App name, GitHub coordinates, plugin version, site URL. |
| `src/lib/layout.shared.tsx` | Nav bar, shared between the docs and home layouts. |
| `src/app/[lang]/(home)` | Landing page. |
| `src/app/[lang]/docs` | Docs layout and pages. |
| `src/app/api/search/route.ts` | Search endpoint, one index per language. |
| `src/proxy.ts` | Locale negotiation plus the Markdown content-negotiation rewrites. Must live beside `app/`, so inside `src/`. |

## Writing content

Pages are MDX with frontmatter:

```mdx
---
title: Page title
description: One line, used for the sidebar and OG image.
icon: Shield
---
```

Ordering and folder names come from `meta.json` in each directory. Icons are
[Lucide](https://lucide.dev) names.

Components available in MDX beyond the defaults: `Tabs`/`Tab`, `Steps`/`Step`,
`Files`/`Folder`/`File`, `Accordions`/`Accordion`, `TypeTable`. Registered in
`src/components/mdx.tsx`.

### Adding a language

1. Add the code to `languages` in `src/lib/i18n.ts` and give it a `displayName` plus UI
   strings.
2. Create `content/docs/<code>/` and mirror the tree.

A page that has no translation falls back to the English one, so a partial translation is
safe to ship.

## Deploying

The project builds as a standard Next.js app — import it into Vercel with the root directory
set to `pklogin-docs`.

Set `NEXT_PUBLIC_SITE_URL` to your production domain so OG images and canonical URLs resolve
against it. Without it, Vercel's own deployment URL is used.
