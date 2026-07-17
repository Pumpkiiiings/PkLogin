package com.pumpkiiings.pkauth.listener.protocol;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.pumpkiiings.pkauth.PkAuthAddon;
import org.bukkit.entity.Player;

/**
 * Bloquea el paquete de spawn de entidades-jugador en el mundo auth.
 * Permite que showPlayer() anada jugadores al tab list sin que sean
 * renderizados visualmente en el mundo.
 */
public class AuthEntityBlocker extends PacketListenerAbstract {

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.SPAWN_ENTITY) return;

        Player receiver = (Player) event.getPlayer();
        if (receiver == null) return;

        PkAuthAddon plugin = PkAuthAddon.getInstance();
        if (plugin == null || plugin.getAuthWorld() == null) return;

        if (!receiver.getWorld().getName().equals(plugin.getAuthWorld().getName())) return;

        WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(event);
        if (packet.getEntityType() == EntityTypes.PLAYER) {
            event.setCancelled(true);
        }
    }
}