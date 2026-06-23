package com.pumpkiiings.pklogin.forge.command.executors;

import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.forge.PkLoginForge;
import com.pumpkiiings.pklogin.forge.command.ForgeAbstractCommand;
import com.pumpkiiings.pklogin.forge.serializer.chat.ChatComponentSerializer;
import net.minecraft.server.level.ServerPlayer;

public class PkLoginCommand extends ForgeAbstractCommand {

    public PkLoginCommand() {
        super("pklogin", false, 2); // Does not require auth, but needs permission level 2 (OP)
    }

    @Override
    protected void perform(ServerPlayer player, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            PkLoginForge.getInstance().setupSettings();
            PkLoginForge.getInstance().getSpawnManager().loadSpawns();
            player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.PLUGIN_RELOAD_MESSAGE.asString("§aPlugin successfully reloaded!")));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("spawnset")) {
            String type = args[1].toLowerCase();
            if (type.equals("join") || type.equals("login") || type.equals("register") || type.equals("respawn")) {
                com.pumpkiiings.pklogin.common.model.Location loc = new com.pumpkiiings.pklogin.common.model.Location(
                        player.level().dimension().location().toString(),
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        player.getYRot(),
                        player.getXRot()
                );
                PkLoginForge.getInstance().getSpawnManager().setSpawn(type, loc);
                player.sendSystemMessage(ChatComponentSerializer.fromText("§aSpawn '" + type + "' has been set to your location."));
            } else {
                player.sendSystemMessage(ChatComponentSerializer.fromText("§cInvalid spawn type! Valid types: join, login, register, respawn."));
            }
        } else {
            player.sendSystemMessage(ChatComponentSerializer.fromText("§eUsage: /pklogin <reload|spawnset <type>>"));
        }
    }
}
