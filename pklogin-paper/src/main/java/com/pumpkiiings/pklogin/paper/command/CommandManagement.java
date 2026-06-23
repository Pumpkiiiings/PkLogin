/*
 * The MIT License (MIT)
 *
 * Copyright © 2020 - 2026 - PkLogin Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.pumpkiiings.pklogin.paper.command;

import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.paper.command.executors.*;
import com.pumpkiiings.pklogin.common.security.filter.LoggerFilterManager;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.PluginCommand;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public class CommandManagement {

    private static final Set<String> ALLOWED_COMMANDS = new HashSet<>();

    private final PkLoginPaper plugin;

    /**
     * Checks if the provided command is allowed.
     *
     * @param command the command to check
     * @return true if is allowed
     */
    public boolean isAllowedCommand(@NonNull String command) {
        return ALLOWED_COMMANDS.contains(command.toLowerCase());
    }

    public void register() {
        try {
            java.lang.reflect.Field commandMapField = org.bukkit.Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) commandMapField.get(org.bukkit.Bukkit.getServer());

            java.lang.reflect.Constructor<PluginCommand> pluginCommandConstructor = PluginCommand.class.getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
            pluginCommandConstructor.setAccessible(true);

            for (Commands command : Commands.values()) {
                PluginCommand pluginCommand = plugin.getCommand(command.name);
                
                if (pluginCommand == null) {
                    pluginCommand = pluginCommandConstructor.newInstance(command.name, plugin);
                    commandMap.register("pklogin", pluginCommand);
                }

                ALLOWED_COMMANDS.add("/" + command.name);
                LoggerFilterManager.addPkLoginCommand("/" + command.name);
                for (String alias : pluginCommand.getAliases()) {
                    ALLOWED_COMMANDS.add("/" + alias.toLowerCase());
                    LoggerFilterManager.addPkLoginCommand("/" + alias.toLowerCase());
                }

                Constructor<?> constructor = command.clasz.getConstructor(PkLoginPaper.class);
                BukkitAbstractCommand bukkitCommand = (BukkitAbstractCommand) constructor.newInstance(plugin);
                pluginCommand.setExecutor(bukkitCommand);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiredArgsConstructor
    public enum Commands {

        CHANGE_PASSWORD("changepassword", ChangePasswordCommand.class),
        LOGIN("login", LoginCommand.class),
        REGISTER("register", RegisterCommand.class),
        OPENLOGIN("pklogin", PkLoginCommand.class),
        UNREGISTER("unregister", UnregisterCommand.class),
        PREMIUM("premium", PremiumCommand.class),
        OFFLINE("offline", OfflineCommand.class),
        TWOFACTOR("2fa", TwoFactorCommand.class);

        private final String name;
        private final Class<?> clasz;

    }

}
