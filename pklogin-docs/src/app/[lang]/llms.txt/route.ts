import { source } from '@/lib/source';
import { llms } from 'fumadocs-core/source';
import { i18n } from '@/lib/i18n';

export const revalidate = false;

export async function GET(_req: Request, { params }: RouteContext<'/[lang]/llms.txt'>) {
  const { lang } = await params;

  return new Response(llms(source).index(lang));
}

export function generateStaticParams() {
  return i18n.languages.map((lang) => ({ lang }));
}
