package com.pumpkiiings.pklogin.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.pumpkiiings.pklogin.common.manager.LoginManagement;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.forge.PkLoginForge;
import com.pumpkiiings.pklogin.forge.serializer.chat.ChatComponentSerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public abstract class ForgeAbstractCommand {

    protected final String commandName;
    protected final boolean requireAuth;
    protected final int permissionLevel;

    public ForgeAbstractCommand(String commandName) {
        this(commandName, false, 0);
    }

    public ForgeAbstractCommand(String commandName, boolean requireAuth, int permissionLevel) {
        this.commandName = commandName;
        this.requireAuth = requireAuth;
        this.permissionLevel = permissionLevel;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(commandName)
                .requires(source -> source.hasPermission(permissionLevel))
                .executes(this::executeWithoutArgs)
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(this::executeWithArgs)));
    }

    private int executeWithoutArgs(CommandContext<CommandSourceStack> context) {
        return executeCommand(context, new String[0]);
    }

    private int executeWithArgs(CommandContext<CommandSourceStack> context) {
        String argsString = StringArgumentType.getString(context, "args");
        String[] args = argsString.split(" ");
        return executeCommand(context, args);
    }

    private int executeCommand(CommandContext<CommandSourceStack> context, String[] args) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer)) {
            context.getSource().sendSuccess(() -> ChatComponentSerializer.fromText(Messages.PLAYER_COMMAND_USAGE.asString()), false);
            return 1;
        }

        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        String name = player.getGameProfile().getName();
        LoginManagement loginManagement = PkLoginForge.getInstance().getLoginManagement();

        if (requireAuth && !loginManagement.isAuthenticated(name)) {
            return 1; // Silently ignore, or could send a message depending on Bukkit's behavior
        }

        if (loginManagement.isUnlocked(name)) {
            // Note: In Bukkit this was async using FoliaLib. We can run it on the server thread or async.
            // For Forge, it's safer to just execute synchronously unless DB operations block.
            // BCrypt operations should ideally be async to prevent lag.
            Thread thread = new Thread(() -> {
                try {
                    perform(player, commandName, args);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            thread.setName("PkLogin-Command-" + commandName);
            thread.start();
        }

        return 1;
    }

    protected abstract void perform(ServerPlayer player, String label, String[] args);
}
