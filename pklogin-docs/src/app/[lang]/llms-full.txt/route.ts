import { getLLMText, source } from '@/lib/source';
import { i18n } from '@/lib/i18n';

export const revalidate = false;

export async function GET(_req: Request, { params }: RouteContext<'/[lang]/llms-full.txt'>) {
  const { lang } = await params;
  const scanned = await Promise.all(source.getPages(lang).map(getLLMText));

  return new Response(scanned.join('\n\n'));
}

export function generateStaticParams() {
  return i18n.languages.map((lang) => ({ lang }));
}
