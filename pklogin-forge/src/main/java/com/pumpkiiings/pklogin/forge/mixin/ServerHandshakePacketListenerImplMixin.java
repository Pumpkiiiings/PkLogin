package com.pumpkiiings.pklogin.forge.mixin;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerHandshakePacketListenerImpl.class)
public class ServerHandshakePacketListenerImplMixin {

    @Shadow @Final private Connection connection;

    @Inject(method = "handleIntention", at = @At("HEAD"))
    private void onHandleIntention(ClientIntentionPacket packet, CallbackInfo ci) {
        String host = packet.getHostName();
        // Store host in channel attributes or a static map using remote address
        com.pumpkiiings.pklogin.forge.manager.UsernameAppenderManager.setHost(connection.getRemoteAddress(), host);
    }
}
