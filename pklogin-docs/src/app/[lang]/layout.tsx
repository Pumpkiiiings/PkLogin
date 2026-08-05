import { RootProvider } from 'fumadocs-ui/provider/next';
import '../global.css';
import { Inter } from 'next/font/google';
import type { Metadata } from 'next';
import { i18n } from '@/lib/i18n';
import { appDescription, appName, siteUrl } from '@/lib/shared';
import { DevWarnings } from '@/components/dev-warnings';

const inter = Inter({
  subsets: ['latin'],
});

export const metadata: Metadata = {
  metadataBase: siteUrl(),
  title: {
    default: appName,
    template: `%s — ${appName}`,
  },
  description: appDescription,
};

export function generateStaticParams() {
  return i18n.languages.map((lang) => ({ lang }));
}

export default async function Layout({ children, params }: LayoutProps<'/[lang]'>) {
  const { lang } = await params;

  return (
    <html lang={lang} className={inter.className} suppressHydrationWarning>
      <body className="flex flex-col min-h-screen">
        <DevWarnings />
        <RootProvider i18n={i18n.provider(lang)}>{children}</RootProvider>
      </body>
    </html>
  );
}
