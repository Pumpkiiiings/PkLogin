package com.pumpkiiings.pklogin.paper.command;

import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.paper.command.executors.*;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandRegistrar {

    public static void register(PkLoginPaper plugin, Commands registrar) {
        // Here we will register all command nodes
        registrar.register(LoginCommandNode.build(plugin), "Login to the server");
        registrar.register(RegisterCommandNode.build(plugin), "Register to the server");
        registrar.register(PkLoginCommandNode.build(plugin), "PkLogin main command");
        registrar.register(ChangePasswordCommandNode.build(plugin), "Change your password");
        registrar.register(OfflineCommandNode.build(plugin), "Toggle offline mode");
        registrar.register(PremiumCommandNode.build(plugin), "Toggle premium mode");
        registrar.register(TwoFactorCommandNode.build(plugin), "Manage two factor authentication");
        registrar.register(UnregisterCommandNode.build(plugin), "Unregister your account");
    }
}
