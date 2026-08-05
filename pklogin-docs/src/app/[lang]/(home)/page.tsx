import Link from 'next/link';
import {
  ArrowRight,
  Database,
  Fingerprint,
  Globe,
  KeyRound,
  Network,
  Puzzle,
  ShieldCheck,
  Timer,
} from 'lucide-react';
import { i18n } from '@/lib/i18n';
import { links, pluginVersion } from '@/lib/shared';

export function generateStaticParams() {
  return i18n.languages.map((lang) => ({ lang }));
}

const copy = {
  en: {
    badge: `PkLogin ${pluginVersion} · MIT · Spigot · Paper · Folia · Velocity`,
    title: 'Complete Minecraft authentication, MIT licensed',
    subtitle:
      'Passwordless premium login, Discord 2FA, Argon2id, PostgreSQL and Velocity. Nothing is held back for a paid tier — you get the whole plugin, and all of its source.',
    primary: 'Read the docs',
    secondary: 'Download',
    featuresTitle: 'What it does',
    quickTitle: 'Up and running',
    quickBody:
      'Drop the jar in, turn the server off online mode, start it. Config files generate themselves.',
    quickLink: 'Full installation guide',
    proxyTitle: 'Behind a Velocity proxy?',
    proxyBody:
      'There is nothing to configure. The proxy mode and the signing key are both read from settings your network already has, so there is no second copy to keep in sync.',
    proxyLink: 'Proxy setup',
    features: [
      {
        icon: Fingerprint,
        title: 'Passwordless premium login',
        body: 'Register a paid nickname and PkLogin asks whether it is yours. One click, one reconnect, and Mojang’s own handshake logs you in from then on — no password, ever again.',
      },
      {
        icon: KeyRound,
        title: 'Six password algorithms',
        body: 'BCrypt, Argon2id, PBKDF2, salted SHA-512/256 and read-only AuthMe SHA256. Change the algorithm any time — existing hashes are re-hashed on the next login.',
      },
      {
        icon: ShieldCheck,
        title: 'Discord 2FA',
        body: 'Link an account through a DM bot and receive a single-use code on every session. Codes expire after five minutes and allow five attempts.',
      },
      {
        icon: Timer,
        title: 'Login sessions',
        body: 'Reconnect soon after leaving and skip the password. One disconnect buys one reconnect, tied to the address the player was on.',
      },
      {
        icon: Database,
        title: 'Five database engines',
        body: 'SQLite and H2 need no configuration. MySQL, MariaDB and PostgreSQL connect to a server. Move between them with one command, without losing anything.',
      },
      {
        icon: Network,
        title: 'Signed proxy messages',
        body: 'Auto-login messages carry an HMAC keyed from the Velocity forwarding secret, and the proxy names any backend that fails its verification check.',
      },
      {
        icon: Globe,
        title: '18 translations',
        body: 'English, Spanish, Portuguese, French, German, Russian, Chinese and more, all editable in plain YAML.',
      },
      {
        icon: Puzzle,
        title: 'Developer API',
        body: 'Async account, security and session services plus Bukkit and Velocity events, so your plugins can react to logins.',
      },
    ],
  },
  es: {
    badge: `PkLogin ${pluginVersion} · MIT · Spigot · Paper · Folia · Velocity`,
    title: 'Autenticación completa para Minecraft, con licencia MIT',
    subtitle:
      'Login premium sin contraseña, 2FA por Discord, Argon2id, PostgreSQL y Velocity. Nada se reserva para una versión de pago: tienes el plugin entero, y todo su código.',
    primary: 'Ver la documentación',
    secondary: 'Descargar',
    featuresTitle: 'Qué hace',
    quickTitle: 'En marcha',
    quickBody:
      'Pon el jar, apaga el online mode del servidor y arranca. Los archivos de configuración se generan solos.',
    quickLink: 'Guía completa de instalación',
    proxyTitle: '¿Detrás de un proxy Velocity?',
    proxyBody:
      'No hay nada que configurar. El modo proxy y la clave de firma se leen de ajustes que tu red ya tiene, así que no hay una segunda copia que mantener sincronizada.',
    proxyLink: 'Configuración del proxy',
    features: [
      {
        icon: Fingerprint,
        title: 'Login premium sin contraseña',
        body: 'Registras un nick de pago y PkLogin te pregunta si es tuyo. Un clic, una reconexión, y desde ahí te entra el propio handshake de Mojang: nunca más una contraseña.',
      },
      {
        icon: KeyRound,
        title: 'Seis algoritmos de contraseña',
        body: 'BCrypt, Argon2id, PBKDF2, SHA-512/256 con sal y AuthMe SHA256 en solo lectura. Cambia el algoritmo cuando quieras: los hashes existentes se rehashean en el siguiente login.',
      },
      {
        icon: ShieldCheck,
        title: '2FA por Discord',
        body: 'Vincula la cuenta con un bot por DM y recibe un código de un solo uso en cada sesión. Los códigos caducan a los cinco minutos y permiten cinco intentos.',
      },
      {
        icon: Timer,
        title: 'Sesiones de login',
        body: 'Reconecta poco después de salir y sáltate la contraseña. Una desconexión compra una reconexión, atada a la dirección que tenía el jugador.',
      },
      {
        icon: Database,
        title: 'Cinco motores de base de datos',
        body: 'SQLite y H2 no necesitan configuración. MySQL, MariaDB y PostgreSQL se conectan a un servidor. Cambia entre ellos con un comando, sin perder nada.',
      },
      {
        icon: Network,
        title: 'Mensajes de proxy firmados',
        body: 'Los mensajes de auto-login llevan un HMAC derivado del secreto de forwarding de Velocity, y el proxy nombra a cualquier backend que falle la verificación.',
      },
      {
        icon: Globe,
        title: '18 traducciones',
        body: 'Inglés, español, portugués, francés, alemán, ruso, chino y más, todas editables en YAML plano.',
      },
      {
        icon: Puzzle,
        title: 'API para desarrolladores',
        body: 'Servicios asíncronos de cuentas, seguridad y sesión más eventos de Bukkit y Velocity, para que tus plugins reaccionen a los logins.',
      },
    ],
  },
} as const;

export default async function HomePage({ params }: PageProps<'/[lang]'>) {
  const { lang } = await params;
  const t = lang === 'es' ? copy.es : copy.en;

  return (
    <main className="flex flex-col">
      <section className="relative overflow-hidden border-b border-fd-border px-4 py-24 text-center sm:py-32">
        <div
          aria-hidden
          className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_at_top,var(--color-fd-primary),transparent_60%)] opacity-10"
        />
        <div className="relative mx-auto flex max-w-3xl flex-col items-center gap-6">
          <span className="rounded-full border border-fd-border bg-fd-card px-3 py-1 text-xs font-medium text-fd-muted-foreground">
            {t.badge}
          </span>
          <h1 className="text-balance text-4xl font-bold tracking-tight sm:text-6xl">{t.title}</h1>
          <p className="text-balance text-lg text-fd-muted-foreground">{t.subtitle}</p>
          <div className="flex flex-wrap items-center justify-center gap-3">
            <Link
              href={`/${lang}/docs`}
              className="inline-flex items-center gap-2 rounded-lg bg-fd-primary px-5 py-2.5 text-sm font-medium text-fd-primary-foreground transition-opacity hover:opacity-90"
            >
              {t.primary}
              <ArrowRight className="size-4" />
            </Link>
            <a
              href={links.releases}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-2 rounded-lg border border-fd-border px-5 py-2.5 text-sm font-medium transition-colors hover:bg-fd-accent"
            >
              {t.secondary}
            </a>
          </div>
        </div>
      </section>

      <section className="mx-auto w-full max-w-6xl px-4 py-16">
        <h2 className="mb-8 text-2xl font-semibold">{t.featuresTitle}</h2>
        <div className="grid gap-px overflow-hidden rounded-xl border border-fd-border bg-fd-border sm:grid-cols-2 lg:grid-cols-4">
          {t.features.map((feature) => (
            <div key={feature.title} className="flex flex-col gap-2 bg-fd-card p-5">
              <feature.icon className="size-5 text-fd-primary" />
              <h3 className="font-medium">{feature.title}</h3>
              <p className="text-sm text-fd-muted-foreground">{feature.body}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="mx-auto grid w-full max-w-6xl gap-6 px-4 pb-24 lg:grid-cols-2">
        <div className="rounded-xl border border-fd-border bg-fd-card p-6">
          <h2 className="text-lg font-semibold">{t.quickTitle}</h2>
          <p className="mt-2 text-sm text-fd-muted-foreground">{t.quickBody}</p>
          <pre className="mt-4 overflow-x-auto rounded-lg bg-fd-secondary p-4 text-xs leading-relaxed">
            <code>{`plugins/PkLogin.jar
server.properties   online-mode=false
/pklogin reload`}</code>
          </pre>
          <Link
            href={`/${lang}/docs/getting-started/installation`}
            className="mt-4 inline-flex items-center gap-1 text-sm font-medium text-fd-primary hover:underline"
          >
            {t.quickLink}
            <ArrowRight className="size-3.5" />
          </Link>
        </div>

        <div className="rounded-xl border border-fd-border bg-fd-card p-6">
          <h2 className="text-lg font-semibold">{t.proxyTitle}</h2>
          <p className="mt-2 text-sm text-fd-muted-foreground">{t.proxyBody}</p>
          <pre className="mt-4 overflow-x-auto rounded-lg bg-fd-secondary p-4 text-xs leading-relaxed">
            <code>{`[PkLogin] Backend 'auth' verified: PkLogin ${pluginVersion},
          matching signing key (14 ms).`}</code>
          </pre>
          <Link
            href={`/${lang}/docs/getting-started/velocity`}
            className="mt-4 inline-flex items-center gap-1 text-sm font-medium text-fd-primary hover:underline"
          >
            {t.proxyLink}
            <ArrowRight className="size-3.5" />
          </Link>
        </div>
      </section>
    </main>
  );
}
