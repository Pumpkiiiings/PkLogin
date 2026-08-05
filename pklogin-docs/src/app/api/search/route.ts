import { source } from '@/lib/source';
import { createFromSource } from 'fumadocs-core/search/server';

// Each language gets its own index with its own stemmer, otherwise Spanish
// queries are tokenised with English rules and stop matching.
export const { GET } = createFromSource(source, {
  localeMap: {
    en: 'english',
    es: 'spanish',
  },
});
