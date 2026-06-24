package com.pumpkiiings.pklogin.forge.command.executors;

import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.forge.PkLoginForge;
import com.pumpkiiings.pklogin.forge.command.ForgeAbstractCommand;
import com.pumpkiiings.pklogin.forge.serializer.chat.ChatComponentSerializer;
import net.minecraft.server.level.ServerPlayer;
import com.pumpkiiings.pklogin.common.model.Account;

import java.util.Optional;

public class PkLoginCommand extends ForgeAbstractCommand {

    public PkLoginCommand() {
        super("pklogin", false, 2); // Does not require auth, but needs permission level 2 (OP)
    }

    @Override
    protected void perform(ServerPlayer player, String label, String[] args) {
        if (args.length == 0) {
            player.sendSystemMessage(ChatComponentSerializer.fromText("§eUsage: /pklogin <reload|setspawn|forcelogin|unregister|delete|verify|changepass|dupeip>"));
            return;
        }

        String subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "reload":
            case "rl":
            case "r": {
                PkLoginForge.getInstance().setupSettings();
                PkLoginForge.getInstance().getSpawnManager().loadSpawns();
                player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.PLUGIN_RELOAD_MESSAGE.asString("§aPlugin successfully reloaded!")));
                break;
            }
            case "spawnset":
            case "setspawn": {
                if (args.length < 2) {
                    player.sendSystemMessage(ChatComponentSerializer.fromText("§eUsage: /pklogin setspawn <join|login|register|respawn>"));
                    return;
                }
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
                    player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.ADMIN_SETSPAWN_SUCCESS.asString()));
                } else {
                    player.sendSystemMessage(ChatComponentSerializer.fromText("§cInvalid spawn type! Valid types: join, login, register, respawn."));
                }
                break;
            }
            case "forcelogin": {
                if (args.length < 2) {
                    player.sendSystemMessage(ChatComponentSerializer.fromText("§eUsage: /pklogin forcelogin <player>"));
                    return;
                }
                String targetName = args[1];
                net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
                ServerPlayer target = server != null ? server.getPlayerList().getPlayerByName(targetName) : null;
                if (target != null) {
                    PkLoginForge.getInstance().getLoginManagement().setAuthenticated(target.getGameProfile().getName());
                    com.pumpkiiings.pklogin.forge.listener.ForgeAuthenticateListener.onAuthenticate(target, false);
                    target.sendSystemMessage(ChatComponentSerializer.fromText(Messages.SUCCESSFUL_LOGIN.asString()));
                    player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.ADMIN_FORCELOGIN_SUCCESS.asString().replace("{0}", targetName)));
                } else {
                    player.sendSystemMessage(ChatComponentSerializer.fromText("§cPlayer not found or not online."));
                }
                break;
            }
            case "unregister": {
                if (args.length < 2) {
                    player.sendSystemMessage(ChatComponentSerializer.fromText("§eUsage: /pklogin unregister <player>"));
                    return;
                }
                String targetName = args[1];
                if (PkLoginForge.getInstance().getAccountManagement().removePassword(targetName)) {
                    PkLoginForge.getInstance().getLoginManagement().cleanup(targetName);
                    player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.ADMIN_UNREGISTER_SUCCESS.asString().replace("{0}", targetName)));
                } else {
                    player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.ADMIN_ACCOUNT_NOT_FOUND.asString()));
                }
                break;
            }
            case "delete": {
                if (args.length < 2) {
                    player.sendSystemMessage(ChatComponentSerializer.fromText("§eUsage: /pklogin delete <player>"));
                    return;
                }
                String targetName = args[1];
                if (PkLoginForge.getInstance().getAccountManagement().delete(targetName)) {
                    PkLoginForge.getInstance().getLoginManagement().cleanup(targetName);
                    player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.ADMIN_DELETE_SUCCESS.asString().replace("{0}", targetName)));
                } else {
                    player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.ADMIN_ACCOUNT_NOT_FOUND.asString()));
                }
                break;
            }
            case "verify": {
                if (args.length < 2) {
                    player.sendSystemMessage(ChatComponentSerializer.fromText("§eUsage: /pklogin verify <player>"));
                    return;
                }
                String targetName = args[1];
                Optional<Account> accOpt = PkLoginForge.getInstance().getAccountManagement().search(targetName);
                if (accOpt.isPresent()) {
                    Account acc = accOpt.get();
                    java.util.List<String> verifyFormat = Messages.ADMIN_VERIFY_FORMAT.asList();
                    for (String line : verifyFormat) {
                        player.sendSystemMessage(ChatComponentSerializer.fromText(line.replace("{0}", targetName)
                                .replace("{1}", acc.getAddress())
                                .replace("{2}", new java.util.Date(acc.getRegDate()).toString())
                                .replace("{3}", new java.util.Date(acc.getLastLogin()).toString())
                                .replace("{4}", (!"OFFLINE".equals(acc.getUuidType()) ? "Yes" : "No"))));
                    }
                } else {
                    player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.ADMIN_ACCOUNT_NOT_FOUND.asString()));
                }
                break;
            }
            case "changepass": {
                if (args.length < 3) {
                    player.sendSystemMessage(ChatComponentSerializer.fromText("§eUsage: /pklogin changepass <player> <newpass>"));
                    return;
                }
                String targetName = args[1];
                String newPass = args[2];
                Optional<Account> targetAccount = PkLoginForge.getInstance().getAccountManagement().search(targetName);
                if (targetAccount.isPresent()) {
                    String hash = new com.pumpkiiings.pklogin.common.security.hashing.BCryptStrategy().hash(newPass);
                    PkLoginForge.getInstance().getAccountManagement().update(targetName, hash, targetAccount.get().getAddress());
                    player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.ADMIN_CHANGEPASS_SUCCESS.asString().replace("{0}", targetName)));
                    net.minecraft.server.MinecraftServer mcServer = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
                    ServerPlayer target = mcServer != null ? mcServer.getPlayerList().getPlayerByName(targetName) : null;
                    if (target != null) {
                        target.connection.disconnect(net.minecraft.network.chat.Component.literal(Messages.ADMIN_CHANGEPASS_KICK.asString()));
                    }
                } else {
                    player.sendSystemMessage(ChatComponentSerializer.fromText("§cAccount not found."));
                }
                break;
            }
            case "dupeip": {
                if (args.length < 2) {
                    player.sendSystemMessage(ChatComponentSerializer.fromText("§eUsage: /pklogin dupeip <ip/player>"));
                    return;
                }
                String target = args[1];
                String ip = target;

                if (!target.contains(".")) {
                    Optional<Account> acc = PkLoginForge.getInstance().getAccountManagement().search(target);
                    if (acc.isPresent()) {
                        ip = acc.get().getAddress();
                    }
                }

                java.util.Map<String, Long> accounts = PkLoginForge.getInstance().getAccountManagement().getAccountsByIp(ip);
                if (accounts.isEmpty()) {
                    player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.ADMIN_DUPEIP_NONE.asString()));
                } else {
                    player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.ADMIN_DUPEIP_HEADER.asString().replace("{0}", target)));
                    accounts.forEach((name, lastLogin) -> {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
                        String dateStr = sdf.format(new java.util.Date(lastLogin));
                        player.sendSystemMessage(ChatComponentSerializer.fromText(Messages.ADMIN_DUPEIP_FORMAT.asString().replace("{0}", name).replace("{1}", dateStr)));
                    });
                }
                break;
            }
            default: {
                player.sendSystemMessage(ChatComponentSerializer.fromText("§eUsage: /pklogin <reload|setspawn|forcelogin|unregister|delete|verify|changepass|dupeip>"));
                break;
            }
        }
    }
}
