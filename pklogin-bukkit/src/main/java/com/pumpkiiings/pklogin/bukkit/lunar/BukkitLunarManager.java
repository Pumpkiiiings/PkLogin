package com.pumpkiiings.pklogin.bukkit.lunar;

import com.pumpkiiings.pklogin.bukkit.PkLoginBukkit;
import com.pumpkiiings.pklogin.common.lunar.LunarPayloads;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class BukkitLunarManager implements PluginMessageListener {
    
    private final PkLoginBukkit plugin;

    public BukkitLunarManager(PkLoginBukkit plugin) {
        this.plugin = plugin;
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, LunarPayloads.CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, LunarPayloads.CHANNEL, this);
        
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, LunarPayloads.LEGACY_CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, LunarPayloads.LEGACY_CHANNEL, this);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (channel.equals(LunarPayloads.CHANNEL) || channel.equals(LunarPayloads.LEGACY_CHANNEL)) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (!target.equals(player) && target.getListeningPluginChannels().contains(channel)) {
                    target.sendPluginMessage(plugin, channel, message);
                }
            }
        }
    }
}
