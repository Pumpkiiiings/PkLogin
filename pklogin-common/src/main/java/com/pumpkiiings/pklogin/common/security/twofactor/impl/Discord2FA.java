package com.pumpkiiings.pklogin.common.security.twofactor.impl;

import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorProvider;

public class Discord2FA implements TwoFactorProvider {

    private boolean enabled;
    private net.dv8tion.jda.api.JDA jda;

    @Override
    public String getProviderId() {
        return "DISCORD";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void init() {
        try {
            java.io.File file = new java.io.File("config/pklogin/2fa/discord.yml");
            if (!file.exists()) file = new java.io.File("plugins/PkLogin/2fa/discord.yml");
            if (!file.exists()) return;

            // Simple yaml parsing
            String token = "";
            boolean isEnabled = false;
            for (String line : java.nio.file.Files.readAllLines(file.toPath())) {
                if (line.trim().startsWith("enable:")) {
                    isEnabled = Boolean.parseBoolean(line.split(":", 2)[1].trim());
                } else if (line.trim().startsWith("token:")) {
                    token = line.split(":", 2)[1].trim().replace("\"", "").replace("'", "");
                }
            }

            this.enabled = isEnabled;
            if (!this.enabled) return;
            if (token == null || token.isEmpty()) {
                System.err.println("[PkLogin] Discord 2FA is enabled but token is missing!");
                return;
            }

            jda = net.dv8tion.jda.api.JDABuilder.createLight(token)
                    .addEventListeners(new DiscordListener())
                    .build();
            
            jda.awaitReady();
            System.out.println("[PkLogin] Discord Bot initialized successfully!");
        } catch (Throwable t) {
            System.err.println("[PkLogin] Failed to initialize Discord Bot: " + t.getMessage());
            this.enabled = false;
        }
    }

    @Override
    public boolean isLinked(Account account) {
        return account.getDiscordId() != null && !account.getDiscordId().isEmpty();
    }

    @Override
    public boolean sendVerificationCode(Account account, String code) {
        if (!enabled || jda == null || !isLinked(account)) return false;
        try {
            net.dv8tion.jda.api.entities.User user = jda.retrieveUserById(account.getDiscordId()).complete();
            if (user != null) {
                user.openPrivateChannel().queue(channel -> {
                    channel.sendMessage("Tu código de verificación de PkLogin es: **" + code + "**").queue();
                });
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void shutdown() {
        if (enabled && jda != null) {
            jda.shutdown();
        }
    }
}
