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
- **Premium / Cracked**: Auto-login for premium (paid) players, manual login for cracked players
- **Username Appender**: Prevent username collisions between premium and cracked players sharing a name
- **Brute-force protection**: Configurable max login attempts before kick
- **IP Account Limit**: Restrict how many accounts can be registered per IP address
- **Login timeout**: Auto-kick players who don't authenticate in time
- **Limbo system**: Players are frozen (movement, commands, chat blocked) until authenticated
- **Safe-location teleport**: Teleport players to a safe spawn on join, restore last location after login

### 🔑 Password Security
| Algorithm | Notes |
|-----------|-------|
| **BCrypt** (default) | Adaptive cost factor — best general choice |
| **Argon2id** | Winner of the Password Hashing Competition, highest security |
| **PBKDF2** | 600,000 iterations (OWASP 2024 recommendation) |
| **SHA-512** | Salted — compatibility mode |
| **SHA-256** | Salted — compatibility mode |
| **AuthMe SHA256** | Read-only — auto-migrates on first login |

> **Zero-downtime migration**: Change `hash-algorithm` in config at any time. Existing hashes are auto-detected and silently re-hashed to the new algorithm on the player's next successful login.

### 🔒 Two-Factor Authentication (2FA)
- **Discord 2FA**: Link your Minecraft account to Discord via DM bot — receive a login code on every session
- **Email 2FA**: SMTP-based verification codes on login
- **TOTP** (Forge): Google Authenticator / Authy compatible (`/2fa setup`)
- Flexible linking flow: `/2fa discord` → get a 5-digit code → DM the bot to link

### 🗄️ Database Support
| Engine | Notes |
|--------|-------|
| **SQLite** | Default, zero-config, local file |
| **H2** | In-process, file-based |
| **MySQL** | External MySQL server |
| **MariaDB** | External MariaDB server |

### 🔄 Migration Support
- **AuthMe → PkLogin**: One-command async bulk import (`/pklogin authme-import`)
  - Preserves passwords (auto-migrated on first login)
  - Preserves IPs, registration dates
  - Skips already-imported accounts
  - Progress updates every 100 accounts

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
| `/2fa setup` | *(Forge)* Set up TOTP (Google Authenticator) |
| `/2fa verify <code>` | *(Forge)* Verify TOTP code |
| `/2fa disable <code>` | *(Forge)* Disable TOTP |

### Admin Commands
| Command | Permission | Description |
|---------|------------|-------------|
| `/pklogin reload` | `pklogin.admin` | Reload config and messages |
| `/pklogin authme-import` | `pklogin.admin` | Import accounts from AuthMe (async) |
| `/pklogin forcelogin <user>` | `pklogin.admin` | Force log in a player |
| `/pklogin unregister <user>` | `pklogin.admin` | Permanently delete a player account |
| `/pklogin changepass <user> <pass>` | `pklogin.admin` | Force-change a player's password |
| `/pklogin dupeip <ip/user>` | `pklogin.admin` | List accounts sharing the same IP |
| `/pklogin update` | `pklogin.admin` | Download latest PkLogin version |

---

## ⚙️ Configuration

```yaml
Security:
  time-to-login: 45          # Seconds to authenticate before kick
  hash-algorithm: BCRYPT     # BCRYPT | ARGON2 | PBKDF2 | SHA512 | SHA256

  password:
    small: 5                 # Min password length
    large: 15                # Max password length

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

premium:
  username-appender:
    enabled: false           # Prevent name collisions premium vs cracked
```

### 2FA — `plugins/PkLogin/2fa/discord.yml`
```yaml
enabled: false
bot-token: "YOUR_BOT_TOKEN_HERE"
```

### 2FA — `plugins/PkLogin/2fa/email.yml`
```yaml
enabled: false
smtp:
  host: "smtp.gmail.com"
  port: 587
  user: "your@email.com"
  password: "your_password"
  encryption: TLS
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
3. Start the server — config files are auto-generated
4. Edit `plugins/PkLogin/config.yml` (or `config/pklogin/config.yml` on Forge)
5. Restart or run `/pklogin reload`

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
