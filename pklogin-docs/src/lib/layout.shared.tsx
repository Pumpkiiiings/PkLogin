import type { BaseLayoutProps } from 'fumadocs-ui/layouts/shared';
import { ShieldCheck } from 'lucide-react';
import { appName, gitConfig, links } from './shared';

const nav = {
  en: { docs: 'Documentation', api: 'Developer API', download: 'Download' },
  es: { docs: 'Documentación', api: 'API para desarrolladores', download: 'Descargar' },
} as const;

function labels(lang: string) {
  return lang === 'es' ? nav.es : nav.en;
}

export function baseOptions(lang: string): BaseLayoutProps {
  const t = labels(lang);

  return {
    nav: {
      url: `/${lang}`,
      title: (
        <>
          <ShieldCheck className="size-5 text-fd-primary" />
          <span className="font-semibold">{appName}</span>
        </>
      ),
    },
    githubUrl: `https://github.com/${gitConfig.user}/${gitConfig.repo}`,
    links: [
      {
        text: t.docs,
        url: `/${lang}/docs`,
        active: 'nested-url',
      },
      {
        text: t.api,
        url: `/${lang}/docs/developers/api`,
        active: 'nested-url',
      },
      {
        type: 'button',
        text: t.download,
        url: links.releases,
        external: true,
      },
    ],
  };
}
