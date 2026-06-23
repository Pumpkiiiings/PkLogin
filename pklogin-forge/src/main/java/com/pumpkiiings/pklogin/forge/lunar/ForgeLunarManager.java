package com.pumpkiiings.pklogin.forge.lunar;

import com.pumpkiiings.pklogin.common.lunar.LunarPayloads;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.event.EventNetworkChannel;

public class ForgeLunarManager {

    private final EventNetworkChannel channel;

    public ForgeLunarManager() {
        String[] split = LunarPayloads.CHANNEL.split(":");
        channel = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(split[0], split[1]))
                .networkProtocolVersion(() -> "1")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .eventNetworkChannel();

        channel.addListener(this::onServerPayload);
    }

    private void onServerPayload(NetworkEvent.ServerCustomPayloadEvent event) {
        ServerPlayer sender = event.getSource().get().getSender();
        if (sender != null) {
            FriendlyByteBuf buf = event.getPayload();
            byte[] data = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), data);

            // Re-broadcast to all other players
            FriendlyByteBuf newBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            String[] split = LunarPayloads.CHANNEL.split(":");
            net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket packet = 
                new net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket(
                    new ResourceLocation(split[0], split[1]), newBuf);

            net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().broadcastAll(packet);
        }
        event.getSource().get().setPacketHandled(true);
    }
}
