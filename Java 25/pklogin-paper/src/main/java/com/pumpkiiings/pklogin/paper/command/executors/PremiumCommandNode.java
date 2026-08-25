package com.pumpkiiings.pklogin.paper.command.executors;

import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.common.manager.AccountManagement;
import com.pumpkiiings.pklogin.common.manager.LoginManagement;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.common.settings.Settings;
import com.pumpkiiings.pklogin.paper.manager.PremiumManager;
import com.pumpkiiings.pklogin.paper.util.AdventureAPI;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class PremiumCommandNode {

    public static LiteralCommandNode<CommandSourceStack> build(PkLoginPaper plugin) {
        return Commands.literal("premium")
            .requires(source -> source.getSender() instanceof Player)
            .executes(context -> {
                CommandSender sender = context.getSource().getSender();
                plugin.runAsync(() -> perform(plugin, (Player) sender, false));
                return 1;
            })
            .then(Commands.literal("confirm")
                .executes(context -> {
                    Player player = (Player) context.getSource().getSender();

                    // The join question ends here too, but that player has no
                    // account and is not logged in, so perform() does not apply.
                    if (PremiumManager.isAwaitingAnswer(player.getName())) {
                        PremiumManager.acceptQuestion(plugin, player, addressOf(player));
                        return 1;
                    }

                    plugin.runAsync(() -> perform(plugin, player, true));
                    return 1;
                })
            )
            // Second step of the question's yes button. Clicking yes lands here
            // rather than on confirm, so that claiming the nickname always takes
            // a deliberate second click: a player who does not own it ends up
            // with an account that has no password and cannot pass the Mojang
            // handshake, and only an admin can undo that.
            .then(Commands.literal("question-confirm")
                .executes(context -> {
                    Player player = (Player) context.getSource().getSender();
                    PremiumManager.confirmQuestion(plugin, player,
                            () -> promptToRegister(plugin, player));
                    return 1;
                })
            )
            // Says no to the question, at either step. Nothing is stored: it only
            // ever fires on a name with no account, so declining cannot come
            // round again.
            .then(Commands.literal("no")
                .executes(context -> {
                    Player player = (Player) context.getSource().getSender();

                    if (PremiumManager.isAwaitingAnswer(player.getName())) {
                        PremiumManager.declineQuestion(plugin, player,
                                () -> promptToRegister(plugin, player));
                        return 1;
                    }

                    player.sendMessage(Messages.PREMIUM_QUESTION_DECLINED.asString(
                            "§7No problem. You will keep logging in with your password."));
                    return 1;
                })
            ).build();
    }

    /** Hands a declining player back to the registration they were held back from. */
    private static void promptToRegister(PkLoginPaper plugin, Player player) {
        PremiumManager.startLoginQueue(player, false);

        player.sendMessage(Messages.MESSAGE_REGISTER.asString());
        if (Settings.UI_TITLE_BAR.asBoolean()) {
            com.pumpkiiings.pklogin.paper.util.AdventureAPI.showTitle(player,
                    Messages.TITLE_BEFORE_REGISTER.asTitle().title,
                    Messages.TITLE_BEFORE_REGISTER.asTitle().subtitle,
                    Messages.TITLE_BEFORE_REGISTER.asTitle().start,
                    Messages.TITLE_BEFORE_REGISTER.asTitle().duration,
                    Messages.TITLE_BEFORE_REGISTER.asTitle().end);
        }
    }

    private static String addressOf(Player player) {
        return player.getAddress() == null || player.getAddress().getAddress() == null
                ? null
                : player.getAddress().getAddress().getHostAddress();
    }

    private static void perform(PkLoginPaper plugin, Player player, boolean confirm) {
        String name = player.getName();
        LoginManagement loginManagement = plugin.getLoginManagement();
        AccountManagement accountManagement = plugin.getAccountManagement();

        if (!loginManagement.isAuthenticated(name)) {
            player.sendMessage(Messages.TWO_FACTOR_NOT_LOGGED_IN_SETUP.asString());
            return;
        }

        Optional<Account> accountOpt = accountManagement.retrieveOrLoad(name);
        if (!accountOpt.isPresent()) {
            player.sendMessage(Messages.TWO_FACTOR_NOT_REGISTERED.asString());
            return;
        }

        Account account = accountOpt.get();
        String currentType = account.getUuidType() != null ? account.getUuidType() : "REAL";

        if (confirm) {
            if (currentType.equals("REAL")) {
                player.sendMessage(Messages.PREMIUM_ALREADY.asString());
                return;
            }
            accountManagement.updateUuidType(name, "REAL");
            accountManagement.invalidateCache(name);
            player.sendMessage(Messages.PREMIUM_SUCCESS.asString());
            player.getScheduler().run(plugin, task -> player.kick(
                    net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                            .deserialize(Messages.PREMIUM_SWITCH_KICK.asString(
                                    "§aYou have switched to Premium mode.\n§ePlease reconnect to the server."))), null);
        } else {
            if (currentType.equals("REAL")) {
                player.sendMessage(Messages.PREMIUM_ALREADY.asString());
            } else {
                for (String msg : Messages.PREMIUM_WARNING.asList()) {
                    player.sendMessage(msg);
                }
            }
        }
    }
}
