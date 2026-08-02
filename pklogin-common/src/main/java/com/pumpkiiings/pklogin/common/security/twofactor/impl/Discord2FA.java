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
    public void init(java.io.File dataFolder) {
        try {
            java.io.File file = new java.io.File(dataFolder, "2fa/discord.yml");
            if (!file.exists()) return;

            // Read the file as YAML rather than scanning lines: "enable" appears
            // both at the root and under options.link-required, and a line-based
            // parser picks up whichever comes last, so the setting never applied.
            dev.dejvokep.boostedyaml.YamlDocument config = dev.dejvokep.boostedyaml.YamlDocument.create(file);

            this.enabled = config.getBoolean("enable", false);
            if (!this.enabled) return;

            String token = config.getString("authentication.token", "");
            if (token == null || token.trim().isEmpty()) {
                System.err.println("[PkLogin] Discord 2FA is enabled but 'authentication.token' is missing!");
                this.enabled = false;
                return;
            }

            jda = net.dv8tion.jda.api.JDABuilder.createLight(token)
                    .addEventListeners(new DiscordListener())
                    .build();

            // Connecting can take several seconds; blocking here would stall
            // server startup, so readiness is awaited off the calling thread.
            new Thread(() -> {
                try {
                    jda.awaitReady();
                    System.out.println("[PkLogin] Discord Bot initialized successfully!");
                } catch (Exception e) {
                    System.err.println("[PkLogin] Discord Bot failed to become ready: " + e.getMessage());
                    this.enabled = false;
                }
            }, "PkLogin-Discord-Init").start();
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
                    channel.sendMessage(com.pumpkiiings.pklogin.common.settings.Messages.DISCORD_BOT_VERIFICATION_CODE
                            .asString("Your PkLogin verification code is: **{0}**")
                            .replace("{0}", code)).queue();
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
