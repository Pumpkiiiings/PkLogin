<div align="center">

<img src="https://i.ibb.co/bjNT8cPW/pkloginlogo.png" alt="PkLogin" width="1200" height="180"/>

# PkLogin

**A practical, secure and feature-rich authentication plugin for Spigot/Paper/Folia and Velocity**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://www.java.com)
[![Spigot](https://img.shields.io/badge/Spigot-1.8--1.21-orange.svg)](https://www.spigotmc.org/)
[![Forge](https://img.shields.io/badge/Forge-1.20.1-green.svg)](https://files.minecraftforge.net/)
[![bStats](https://img.shields.io/bstats/servers/placeholder?label=Servers)](https://bstats.org/)

</div>

---

## ✨ Features

### 🔐 Authentication
- **Dual-mode**: Works on both **Spigot/Paper/Folia and Velocity**
- **Passwordless premium login**: Premium players never type a password, not even on
  their first join — see [below](#passwordless-premium-login)
- **Login sessions**: Reconnect soon after leaving and skip the password —
  see [below](#login-sessions)
- **Username Appender**: Prevent username collisions between premium and cracked players sharing a name
- **Brute-force protection**: Configurable max login attempts before kick
- **IP Account Limit**: Restrict how many accounts can be registered per IP address
- **Login timeout**: Auto-kick players who don't authenticate in time
- **Limbo system**: Players are frozen (movement, commands, chat blocked) until authenticated
- **Safe-location teleport**: Teleport players to a safe spawn on join, restore last location after login

### 🛰️ Proxy
- **Nothing to configure**: neither a proxy-mode switch nor a shared secret — both
  are read from settings your network already has
- **Signed messages**: the proxy's auto-login messages carry an HMAC keyed by a
  secret derived from Velocity's forwarding secret, so a client cannot forge one
- **Backend verification**: the proxy checks that each auth server runs PkLogin and
  resolved the same key, and names the ones that do not

### 🔑 Password Security
| Algorithm | Notes |
|-----------|-------|
| **BCrypt** (default) | Adaptive cost factor — best general choice |
| **Argon2id** | Winner of the Password Hashing Competition, highest security |
| **PBKDF2** | 600,000 iterations (OWASP 2024 recommendation) |
| **SHA-512** | Salted — compatibility mode |
| **SHA-256** | Salted — compatibility mode |
| **AuthMe SHA256** | Read-only — auto-migrates on first login |

> **Zero-downtime migration**: Change `Security.hash-algorithm` in config at any time. Existing hashes are auto-detected and silently re-hashed to the new algorithm on the player's next successful login.

### 🔒 Two-Factor Authentication (2FA)
- **Discord 2FA**: Link your Minecraft account to Discord via DM bot — receive a login code on every session
- Linking flow: `/2fa discord` → get a 6-digit code → DM the bot to link
- Codes are single-use, expire after 5 minutes and allow 5 attempts

> **Email 2FA** ships with the plugin but is not yet wired into the login flow, and TOTP requires the Forge module, which is not part of this repository. Both are tracked as future work.

### 🗄️ Database Support
| Engine | Notes |
|--------|-------|
| **SQLite** | Default, zero-config, local file |
| **H2** | In-process, file-based |
| **MySQL** | External MySQL server |
| **MariaDB** | External MariaDB server |
| **PostgreSQL** | External PostgreSQL server (port 5432, not the 3306 in the default config) |

### 🔄 Migration Support
- **AuthMe → PkLogin**: One-command async bulk import (`/pklogin authme-import`)
  - Preserves passwords (auto-migrated on first login), IPs and registration dates
  - Skips already-imported accounts; progress updates every 100
- **Between engines**: `/pklogin migrate <engine>` copies every account into another
  engine, so switching is not a fresh start
  - Fill in the target's connection details under `Database` in `config.yml`, run the
    command, then change `Database.type` and restart
  - Nothing is deleted and `config.yml` is not touched — the original database stays
    exactly as it was
  - Safe to re-run: accounts already present in the target are skipped

### 🌍 Internationalization
20+ built-in translations: English, Spanish, Portuguese, French, German, Russian, Chinese, Polish, Italian, Turkish, Vietnamese, and more.

### 📡 UUID Types
Configure per-account UUID generation:
- `REAL` — Mojang's official UUID (recommended)
- `RANDOM` — Random UUID per account
- `OFFLINE` — Vanilla offline-mode UUID

---

## 🛠️ Commands

### Player Commands
| Command | Description |
|---------|-------------|
| `/login <password>` | Authenticate with your password |
| `/register <pass> <confirm>` | Register a new account |
| `/changepassword <old> <new>` | Change your password |
| `/unregister <password>` | Delete your account |
| `/premium confirm` | Change your account to REAL mode |
| `/offline` | Change your account to OFFLINE mode |
| `/2fa discord` | Generate a code to link your Discord |
| `/2fa verify2fa <code>` | Enter the 2FA code sent to you |

### Admin Commands

Each subcommand has its own permission, so access can be granted individually.

| Command | Permission | Description |
|---------|------------|-------------|
| `/pklogin help` | `pklogin.admin.help` | Show the admin help |
| `/pklogin reload` | `pklogin.admin.reload` | Reload config and messages |
| `/pklogin authme-import` | `pklogin.admin.authme-import` | Import accounts from AuthMe (async) |
| `/pklogin migrate <engine>` | `pklogin.admin.migrate` | Copy every account into another database engine (async) |
| `/pklogin forcelogin <user>` | `pklogin.admin.forcelogin` | Force log in a player |
| `/pklogin unregister <user>` | `pklogin.admin.unregister` | Clear a player's password |
| `/pklogin delete <user>` | `pklogin.admin.delete` | Permanently delete a player account |
| `/pklogin changepass <user> <pass>` | `pklogin.admin.changepass` | Force-change a player's password |
| `/pklogin verify <user>` | `pklogin.admin.verify` | Show an account's details |
| `/pklogin dupeip <ip/user>` | `pklogin.admin.dupeip` | List accounts sharing the same IP |
| `/pklogin setspawn` | `pklogin.admin.setspawn` | Set the pre-login spawn location |
| `/pklogin update` | `pklogin.admin.update` | Download latest PkLogin version |

---

## ⚙️ Configuration

```yaml
Security:
  time-to-login: 45          # Seconds to authenticate before kick
  hash-algorithm: BCRYPT     # BCRYPT | ARGON2 | PBKDF2 | SHA512 | SHA256

  password:
    small: 5                 # Min password length (inclusive)
    large: 15                # Max password length (inclusive)

  session:
    enable: true             # Skip the password on a quick reconnect
    timeout: 5               # Minutes a session stays open (0 = off)

autologin:
  premium:
    enable: true             # Let premium players in with no password at all
    cache-minutes: 60        # How long a Mojang answer about a name is reused

  bedrock:
    enable: true             # Log Bedrock players in once Floodgate authenticated them
    skip-register: true

passwords:
  bruteforce:
    max-login-tries: 3       # Failed attempts before kick

security:
  ip-limit:
    enable: true
    limit: 3                 # Max accounts per IP

teleport:
  safe-location: true        # Teleport to spawn on join
  last-location: true        # Restore position after login

limbo:
  blindness-effect: false    # Apply blindness before login

username-appender:
  enabled: false             # Prevent name collisions premium vs cracked
```

### Passwordless premium login

A premium player types no password, not even on their first connection. They join
and play.

Before deciding how to negotiate a connection, PkLogin asks Mojang whether the name
belongs to a paid account. If it does, the connection is negotiated as **online
mode** and the client has to complete Mojang's encryption handshake. Passing that is
cryptographic proof of ownership, not a guess — nobody can fake it without the
account. PkLogin then creates the account with no password, because there is nothing
to remember: every later login proves itself the same way.

**This only applies to names PkLogin has never seen.** Anyone already in the database
keeps the mode their account says, so turning this on cannot lock out a player who is
on your server today. An offline account that is really a paid one is converted with
`/premium confirm`, as before.

**When Mojang cannot be reached**, the connection is negotiated as offline and the
player registers a password as usual. That direction is deliberate: a wrong "offline"
costs a password, a wrong "online" costs the player their ability to connect at all.

> **Standalone servers need a packet library.** On Velocity this works out of the
> box. A Paper server with no proxy needs **PacketEvents** or **ProtocolLib**,
> because with `online-mode=false` the server never asks anyone to authenticate with
> Mojang and Paper offers no way to ask just one player to — those libraries provide
> it. Without one, the console says so at startup and premium players fall back to
> `/register`.

### Login sessions

A player who reconnects soon after leaving skips the password. The session opens when
they disconnect, is tied to the address they were on, and is spent the first time it
is used — one disconnect buys one reconnect.

```yaml
Security:
  session:
    enable: true
    timeout: 5     # minutes
```

Sessions are dropped when the password changes, when 2FA is turned on or off, when
the account is deleted, and on `/pklogin reload`. They live in memory, so restarting
the server ends all of them.

> **Know what the address check proves before raising the timeout.** An address is
> not a person: everyone behind one router, one public network or one mobile carrier
> shares it. While a session is open, anyone on that address who types the player's
> name gets in without the password — and without the 2FA code if the account has
> one. Minutes are a reasonable bet on a dropped connection; hours are a standing
> invitation.

> On a network with **several auth servers**, a session opened on one is not known to
> the others, since it lives in that server's memory.

### Proxy setup

**There is nothing to configure in `config.yml` for a proxy.** Not whether this
server is behind one, and not a shared secret. Both are read from settings your
network already has, so there is no second copy to keep in sync and no way for the
two to disagree.

When PkLogin runs on a proxy, the proxy tells each backend when a premium player
has been authenticated. The game client can send messages on that same channel and
the backend cannot tell the two apart, so those messages are signed. The signing
key is derived from the Velocity modern-forwarding secret the proxy and backends
already share (`forwarding.secret` on the proxy, `proxies.velocity.secret` in the
backend's `config/paper-global.yml`). The forwarding secret is never reused
directly — a separate key is derived from it, so the two never share key material.

Whether a backend checks premium accounts itself is likewise not a setting: behind
any forwarding mode the proxy owns the login handshake, so the backend has nothing
left to verify with. PkLogin reads `proxies.velocity.enabled` from
`config/paper-global.yml` (or `settings.bungeecord` from `spigot.yml`) and acts
accordingly.

The console states what was resolved on startup:

```
[PkLogin] Proxy messages authenticated using the Velocity modern forwarding secret.
```

On the proxy, each server listed under `backend.auth-servers` in `backend.yml` is
asked to identify itself the first time a player reaches it. A server that answers
correctly proves it runs PkLogin and resolved the same key:

```
[PkLogin] Backend 'auth' verified: PkLogin 2.0.0, matching signing key (14 ms).
```

One that does not is named in the log, with the reason. Turn the check off with
`backend.verify-connection: false` if a listed server deliberately runs without
PkLogin.

> Without Velocity modern forwarding there is no secret to derive a key from, and
> premium auto-login stays off. This is not a limitation worth working around: a
> network on legacy forwarding accepts whatever identity a client claims on any
> backend port that is reachable, so a shared password between plugins would not
> make it safe. Use modern forwarding, and firewall your backend ports.

### 2FA — `plugins/PkLogin/2fa/discord.yml`
```yaml
enable: false
authentication:
  token: "YOUR_BOT_TOKEN_HERE"
```

---

## 🔌 API (for Developers)

### Gradle
```groovy
repositories {
    maven { url = uri('https://repo.pumpkiiings.com/maven-releases/') }
}

dependencies {
    compileOnly('com.pumpkiiings.pklogin:pklogin-universal:1.4')
}
```

### Maven
```xml
<repositories>
  <repository>
    <id>pumpkiiings-repo</id>
    <url>https://repo.pumpkiiings.com/maven-releases/</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.pumpkiiings.pklogin</groupId>
    <artifactId>pklogin-universal</artifactId>
    <version>1.4</version>
    <scope>provided</scope>
  </dependency>
</dependencies>
```

### Usage Example
```java
PkLoginAPI api = PkLoginAPIProvider.getApi();

// Check if a player is authenticated
boolean auth = api.isAuthenticated("Steve");

// Register a player
api.update("Steve", "myPassword", "127.0.0.1", false);

// Compare password
boolean correct = api.comparePassword("Steve", "myPassword");

// Get account
Optional<Account> account = api.getAccountManagement().retrieveOrLoad("Steve");
```

---

## 🚀 Installation

1. Download `PkLogin-XXXXX-X.X.jar` from [Releases](https://github.com/Pumpkiiiings/PkLogin/releases)
2. Drop it into your `plugins/` (Spigot/Paper) or `mods/` (Forge) folder
3. Set `online-mode=false` in `server.properties` — with it on, the server
   authenticates everyone against Mojang itself and no cracked player can join
4. On a **standalone** server, install **PacketEvents** or **ProtocolLib** as well,
   or premium players will have to register a password
5. Start the server — config files are auto-generated
6. Edit `plugins/PkLogin/config.yml` (or `config/pklogin/config.yml` on Forge)
7. Restart or run `/pklogin reload`

### Behind a Velocity proxy

1. Install PkLogin on the proxy **and** on every server listed under
   `backend.auth-servers` in the proxy's `backend.yml`
2. Use modern forwarding: `player-info-forwarding-mode = "modern"` in `velocity.toml`,
   and the same secret under `proxies.velocity` in each backend's
   `config/paper-global.yml`
3. Firewall the backend ports so nobody can bypass the proxy
4. Start it up — the proxy reports which backends answered its verification check

No proxy settings exist in `config.yml`. There is nothing to fill in.

### Migrating from AuthMe
```
/pklogin authme-import
```
Place your `authme.db` in `plugins/AuthMe/authme.db` before running the command.

---

## 📊 Stats

![bStats](https://bstats.org/signatures/bukkit/PkLogin.svg)

Powered by [bStats](https://bstats.org/)

---

## 📄 License

[MIT License](LICENSE) — © 2020–2026 PkLogin Contributors
