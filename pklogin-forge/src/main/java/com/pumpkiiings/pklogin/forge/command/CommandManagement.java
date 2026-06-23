package com.pumpkiiings.pklogin.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.pumpkiiings.pklogin.common.security.filter.LoggerFilterManager;
import com.pumpkiiings.pklogin.forge.command.executors.*;
import net.minecraft.commands.CommandSourceStack;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

public class CommandManagement {

    private static final Set<String> ALLOWED_COMMANDS = new HashSet<>();

    public boolean isAllowedCommand(String command) {
        return ALLOWED_COMMANDS.contains(command.toLowerCase());
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        for (Commands command : Commands.values()) {
            try {
                ALLOWED_COMMANDS.add(command.name);
                LoggerFilterManager.addPkLoginCommand("/" + command.name);

                Constructor<?> constructor = command.clasz.getConstructor();
                ForgeAbstractCommand forgeCommand = (ForgeAbstractCommand) constructor.newInstance();
                forgeCommand.register(dispatcher);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public enum Commands {
        CHANGE_PASSWORD("changepassword", ChangePasswordCommand.class),
        LOGIN("login", LoginCommand.class),
        REGISTER("register", RegisterCommand.class),
        OPENLOGIN("pklogin", PkLoginCommand.class),
        UNREGISTER("unregister", UnregisterCommand.class),
        TWO_FACTOR("2fa", TwoFactorCommand.class),
        PREMIUM("premium", PremiumCommand.class),
        OFFLINE("offline", OfflineCommand.class);

        private final String name;
        private final Class<?> clasz;

        Commands(String name, Class<?> clasz) {
            this.name = name;
            this.clasz = clasz;
        }
    }
}
