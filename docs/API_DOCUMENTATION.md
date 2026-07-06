# PkLogin Developer API

Welcome to the **PkLogin API Documentation**. This guide will show you how to interact with PkLogin from your own plugins on Bukkit, Paper, or Velocity.

## Table of Contents
1. [Getting Started](#getting-started)
2. [Service Providers](#service-providers)
3. [Account Management API](#account-management-api)
4. [Security API](#security-api)
5. [Session API](#session-api)
6. [Events](#events)

---

## Getting Started

To use the PkLogin API in your project, add the pklogin-api module to your dependencies (depending on how you host or build your project).

The central entry point for the API is the PkLoginProvider class.

`java
import com.pumpkiiings.pklogin.api.service.PkLoginProvider;
import com.pumpkiiings.pklogin.api.service.AccountManagerAPI;

public class MyPlugin {
    public void doSomething() {
        AccountManagerAPI accountManager = PkLoginProvider.getAccountManager();
        // Use the API...
    }
}
`

## Service Providers

PkLogin exposes its core functionality through three main services:

- **AccountManagerAPI**: Used to retrieve account data, check registrations, and delete accounts.
- **SecurityAPI**: Used to compare passwords safely (hashed) and change player passwords.
- **SessionAPI**: Used to check if a player is currently authenticated and to force a login.

All service methods that query the database are fully **asynchronous** and return CompletableFuture<T>.

---

## Account Management API

`java
AccountManagerAPI accountManager = PkLoginProvider.getAccountManager();
`

### Methods:
- CompletableFuture<Optional<AccountData>> getAccount(String name)
  Retrieves the account data for the specified player name. Returns an empty Optional if the player is not registered.
- CompletableFuture<Optional<AccountData>> getAccount(UUID uuid)
  Retrieves the account data for the specified player UUID.
- CompletableFuture<Boolean> isRegistered(String name)
  Checks if the specified player name is registered in the database.
- CompletableFuture<Void> deleteAccount(String name)
  Deletes the specified player's account from the database.

---

## Security API

`java
SecurityAPI securityAPI = PkLoginProvider.getSecurityAPI();
`

### Methods:
- CompletableFuture<Boolean> comparePassword(String name, String rawPassword)
  Safely compares a raw password against the hashed password stored in the database for a player.
- CompletableFuture<Boolean> changePassword(String name, String newPassword)
  Updates the password for the specified player. Automatically handles hashing the new password.

---

## Session API

`java
SessionAPI sessionAPI = PkLoginProvider.getSessionAPI();
`

### Methods:
- oolean isAuthenticated(String name)
  Checks if the specified player is currently logged in on the server. (Synchronous)
- oolean isAuthenticated(UUID uuid)
  Checks if the specified player UUID is currently logged in. (Synchronous)
- CompletableFuture<Boolean> forceLogin(String name)
  Forces the specified player to be authenticated, bypassing the login screen.

---

## Events

PkLogin provides a set of events for both **Bukkit/Paper** and **Velocity**.

### Bukkit / Paper Events

Available in the package: com.pumpkiiings.pklogin.api.event.bukkit.auth

- **PreLoginEvent**
  Fired asynchronously when a player connects to the server (AsyncPlayerPreLoginEvent). 
  _Usage: Listen to this to know when a player is attempting to join before authentication._
  
- **PlayerAuthLoginEvent**
  Fired when a player successfully logs in or registers.
  _Usage: Listen to this to perform actions after a player has proven their identity._

- **PlayerPasswordChangeEvent**
  Fired when a player successfully changes their password in-game.

### Velocity Events

Available in the package: com.pumpkiiings.pklogin.api.event.velocity.auth

- **VelocityPreLoginEvent**
  Fired during the Velocity PreLoginEvent handshake.
  
- **VelocityPlayerAuthLoginEvent**
  Fired when a player successfully authenticates on the backend and the proxy is notified.

- **VelocityPlayerPasswordChangeEvent**
  Fired when a player changes their password, and the proxy intercepts the event.

