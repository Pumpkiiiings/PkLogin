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
import com.pumpkiiings.pklogin.paper.command.executors.PkLoginCommand;
import com.pumpkiiings.pklogin.common.manager.LoginManagement;
import com.pumpkiiings.pklogin.common.settings.Messages;
import lombok.NonNull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class BukkitAbstractCommand implements CommandExecutor {

    protected final PkLoginPaper plugin;
    private final boolean requireAuth;
    private final String permission;

    public BukkitAbstractCommand(PkLoginPaper plugin, @NonNull String command) {
        this(plugin, false, command);
    }

    public BukkitAbstractCommand(PkLoginPaper plugin, boolean requireAuth, @NonNull String command) {
        this.plugin = plugin;
        this.requireAuth = requireAuth;
        this.permission = "pklogin.command." + command.toLowerCase();
    }

    public boolean onCommand(CommandSender sender, Command cmd, String lb, String[] args) {
        final String name = sender.getName();
        final LoginManagement loginManagement = plugin.getLoginManagement();

        if (requireAuth && sender instanceof Player && !loginManagement.isAuthenticated(name)) {
            return true;
        }

        if (!sender.hasPermission(permission)) {
            sender.sendMessage(Messages.INSUFFICIENT_PERMISSIONS.asString());
            return true;
        }

        if (loginManagement.isUnlocked(name)) {
            plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
                try {
                    perform(sender, lb, args);
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.sendMessage("§cFailed to perform the command '" + lb + "', sender: " + sender.getName());
                }
            });
        }
        return true;
    }

    protected abstract void perform(CommandSender sender, String lb, String[] args);
}

