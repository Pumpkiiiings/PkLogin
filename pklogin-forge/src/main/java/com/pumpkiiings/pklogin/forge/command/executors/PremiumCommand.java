package com.pumpkiiings.pklogin.forge.command.executors;

import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.manager.LoginManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.forge.PkLoginForge;
import com.pumpkiiings.pklogin.forge.command.ForgeAbstractCommand;
import com.pumpkiiings.pklogin.forge.serializer.chat.ChatComponentSerializer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class PremiumCommand extends ForgeAbstractCommand {

    public PremiumCommand() {
        super("premium");
    }

    @Override
    protected void perform(ServerPlayer player, String label, String[] args) {
        String name = player.getGameProfile().getName();
        LoginManagement loginManagement = PkLoginForge.getInstance().getLoginManagement();
        AccountManagement accountManagement = PkLoginForge.getInstance().getAccountManagement();

        if (!loginManagement.isAuthenticated(name)) {
            player.sendSystemMessage(ChatComponentSerializer.fromText("§cYou must be logged in to use this command."));
            return;
        }

        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);
        if (!accountOpt.isPresent()) {
            player.sendSystemMessage(ChatComponentSerializer.fromText("§cYou must be registered to use this command."));
            return;
        }

        Account account = accountOpt.get();
        String currentType = account.getUuidType() != null ? account.getUuidType() : "REAL";

        if (args.length == 1) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("confirm")) {
                if (currentType.equals("REAL") || currentType.equals("PREMIUM")) {
                    player.sendSystemMessage(ChatComponentSerializer.fromText("§cYa eres un usuario premium."));
                    return;
                }
                accountManagement.updateUuidType(name, "REAL");
                accountManagement.invalidateCache(name);
                player.sendSystemMessage(ChatComponentSerializer.fromText("§a¡Tu cuenta ahora es Premium!"));
                player.sendSystemMessage(ChatComponentSerializer.fromText("§eA partir de tu próximo inicio de sesión ya no necesitarás usar /login."));
            } else if (subCommand.equals("cracked")) {
                if (currentType.equals("OFFLINE") || currentType.equals("RANDOM")) {
                    player.sendSystemMessage(ChatComponentSerializer.fromText("§cTu cuenta ya es No-Premium (Cracked)."));
                    return;
                }
                accountManagement.updateUuidType(name, "OFFLINE");
                accountManagement.invalidateCache(name);
                player.sendSystemMessage(ChatComponentSerializer.fromText("§a¡Tu cuenta ahora es No-Premium (Cracked)!"));
                player.sendSystemMessage(ChatComponentSerializer.fromText("§eVolverás a usar /login la próxima vez que entres."));
            } else {
                player.sendSystemMessage(ChatComponentSerializer.fromText("§cUso incorrecto."));
            }
        } else {
            if (currentType.equals("REAL") || currentType.equals("PREMIUM")) {
                player.sendSystemMessage(ChatComponentSerializer.fromText("§eActualmente eres un usuario §aPremium§e."));
                player.sendSystemMessage(ChatComponentSerializer.fromText("§eSi quieres volver a ser No-Premium, usa: §c/premium cracked"));
            } else {
                player.sendSystemMessage(ChatComponentSerializer.fromText("§c¡ATENCIÓN! §eSi activas el modo premium, entrarás sin usar /login."));
                player.sendSystemMessage(ChatComponentSerializer.fromText("§4Pero si NO eres premium original, ¡perderás tu cuenta para siempre!"));
                player.sendSystemMessage(ChatComponentSerializer.fromText("§ePara confirmar que eres premium, escribe: §a/premium confirm"));
            }
        }
    }
}
