package com.pumpkiiings.pklogin.velocity.command;

import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.velocity.PkLoginVelocity;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class VelocityAbstractCommand implements SimpleCommand {

    protected final PkLoginVelocity plugin;
    private final boolean requiresAuth;
    private final String permission;

    public VelocityAbstractCommand(PkLoginVelocity plugin, boolean requiresAuth, String permission) {
        this.plugin = plugin;
        this.requiresAuth = requiresAuth;
        this.permission = permission;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        
        if (permission != null && !permission.isEmpty() && !source.hasPermission(permission)) {
            sendMessage(source, Messages.INSUFFICIENT_PERMISSIONS.asString());
            return;
        }

        if (source instanceof Player) {
            Player player = (Player) source;
            if (requiresAuth && !plugin.getAuthenticatedPlayers().contains(player.getUniqueId())) {
                if (invocation.alias().equalsIgnoreCase("2fa") && invocation.arguments().length > 0 && invocation.arguments()[0].equalsIgnoreCase("verify2fa")) {
                    // Forward verify2fa to the backend server to handle limbo removal
                    player.spoofChatInput("/" + invocation.alias() + " " + String.join(" ", invocation.arguments()));
                    return;
                }
                sendMessage(source, Messages.TWO_FACTOR_NOT_LOGGED_IN_SETUP.asString());
                return;
            }
            performPlayer(player, invocation.alias(), invocation.arguments());
        } else {
            performConsole(source, invocation.alias(), invocation.arguments());
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(List.of());
    }

    protected abstract void performPlayer(Player player, String alias, String[] args);
    protected abstract void performConsole(CommandSource console, String alias, String[] args);

    protected void sendMessage(CommandSource source, String message) {
        if (message == null || message.isEmpty()) return;
        source.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
    }
}
