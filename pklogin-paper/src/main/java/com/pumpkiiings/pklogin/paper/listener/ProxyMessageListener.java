package com.pumpkiiings.pklogin.paper.listener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.paper.task.LoginQueue;
import com.pumpkiiings.pklogin.common.settings.Messages;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class ProxyMessageListener implements PluginMessageListener {

    private final PkLoginPaper plugin;

    public ProxyMessageListener(PkLoginPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("pklogin:main")) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if ("PremiumAutoLogin".equals(subChannel)) {
            String username = in.readUTF();
            
            if (player.getName().equalsIgnoreCase(username)) {
                plugin.getLoginManagement().setAuthenticated(player.getName());
                player.sendMessage(Messages.PREMIUM_AUTO_LOGIN.asString());
                LoginQueue.removeFromQueue(player.getName());
                plugin.getServer().getAsyncScheduler().runNow(plugin, task -> new com.pumpkiiings.pklogin.paper.api.events.AsyncAuthenticateEvent(player).callEvt());
            }
        }
    }
}
