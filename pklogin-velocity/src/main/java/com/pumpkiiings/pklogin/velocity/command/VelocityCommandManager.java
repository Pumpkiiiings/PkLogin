package com.pumpkiiings.pklogin.velocity.command;

import com.pumpkiiings.pklogin.velocity.PkLoginVelocity;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;

public class VelocityCommandManager {

    private final PkLoginVelocity plugin;

    public VelocityCommandManager(PkLoginVelocity plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        CommandManager commandManager = plugin.getServer().getCommandManager();

        register(commandManager, "changepassword", new ChangePasswordCommand(plugin));
        register(commandManager, "premium", new PremiumCommand(plugin));
        register(commandManager, "2fa", new TwoFactorCommand(plugin));
        register(commandManager, "offline", new OfflineCommand(plugin));
        register(commandManager, "pklogin", new PkLoginAdminCommand(plugin), "openlogin");
    }

    private void register(CommandManager manager, String name, com.velocitypowered.api.command.SimpleCommand command, String... aliases) {
        CommandMeta meta = manager.metaBuilder(name)
                .aliases(aliases)
                .plugin(plugin)
                .build();
        manager.register(meta, command);
    }
}
