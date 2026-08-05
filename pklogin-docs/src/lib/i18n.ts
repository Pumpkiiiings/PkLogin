import { defineI18nUI } from 'fumadocs-ui/i18n';

/**
 * `parser: 'dir'` keeps each language in its own folder (`content/docs/en`,
 * `content/docs/es`) so a page can exist in one language and not the other
 * without dot-suffixed filenames scattered through the tree.
 */
export const i18n = defineI18nUI(
  {
    languages: ['en', 'es'],
    defaultLanguage: 'en',
    parser: 'dir',
    hideLocale: 'never',
  },
  {
    en: {
      displayName: 'English',
    },
    es: {
      displayName: 'Español',
      'Search(search dialog)': 'Buscar',
      'Search(search trigger)': 'Buscar',
      'Open Search(search trigger)(aria-label)': 'Abrir búsqueda',
      'Close Search(search dialog)(aria-label)': 'Cerrar búsqueda',
      'No results found(search dialog)': 'Sin resultados',
      'On this page(table of contents)': 'En esta página',
      'No Headings(table of contents)': 'Sin encabezados',
      'Table of Contents(inline table of contents)': 'Índice',
      'Next Page(pagination)': 'Siguiente',
      'Previous Page(pagination)': 'Anterior',
      'Last updated on(page footer)': 'Última actualización',
      'Edit on GitHub(edit page)': 'Editar en GitHub',
      'Choose a language(language switcher)': 'Idioma',
      'Choose a language(language switcher)(aria-label)': 'Elegir un idioma',
      'Toggle Theme(theme switcher)(aria-label)': 'Cambiar tema',
      'Light(theme switcher)(aria-label)': 'Claro',
      'Dark(theme switcher)(aria-label)': 'Oscuro',
      'System(theme switcher)(aria-label)': 'Sistema',
      'Show Sidebar(sidebar)': 'Mostrar menú',
      'Hide Sidebar(sidebar)': 'Ocultar menú',
      'Open Sidebar(aria-label)': 'Abrir menú',
      'Close Sidebar(aria-label)': 'Cerrar menú',
      'Open Sidebar(sidebar)(aria-label)': 'Abrir menú',
      'Close Sidebar(sidebar)(aria-label)': 'Cerrar menú',
      'Collapse Sidebar(sidebar)(aria-label)': 'Contraer menú',
      'Toggle Menu(home layout header)(aria-label)': 'Abrir menú',
      'Copy Text(code block)(aria-label)': 'Copiar',
      'Copied Text(code block)(aria-label)': 'Copiado',
      'Copy Anchor Link(heading anchor)(aria-label)': 'Copiar enlace',
      'Copy Link(accordion)(aria-label)': 'Copiar enlace',
      'Copy Markdown(page actions)': 'Copiar Markdown',
      'View as Markdown(page actions)': 'Ver como Markdown',
      'Open(page actions)': 'Abrir',
      'Open in GitHub(page actions)': 'Abrir en GitHub',
      'Page Not Found(404 not found page)': 'Página no encontrada',
      'Back to Home(404 not found page)': 'Volver al inicio',
      'The page you are looking for might have been removed, had its name changed, or is temporarily unavailable.(404 not found page)':
        'La página que buscas puede haber sido eliminada, renombrada o no está disponible temporalmente.',
      'Type(type table)': 'Tipo',
      'Prop(type table)': 'Propiedad',
      'Default(type table)': 'Por defecto',
      'Parameters(type table)': 'Parámetros',
      'Returns(type table)': 'Devuelve',
      'Close Banner(banner)(aria-label)': 'Cerrar aviso',
    },
  },
);
