package com.pumpkiiings.pklogin.common.security.twofactor.impl;

import com.pumpkiiings.pklogin.common.PkLogin;
import com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorManager;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.channel.ChannelType;

public class DiscordListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromType(ChannelType.PRIVATE)) return;

        String message = event.getMessage().getContentRaw().trim();
        
        // Is it a linking code?
        if (message.length() == 5 && message.matches("\\d+")) {
            TwoFactorManager.PendingLink link = TwoFactorManager.getInstance().getPendingLink(message);
            if (link != null && link.providerId.equals("DISCORD")) {
                String discordId = event.getAuthor().getId();
                
                // Link in database
                if (PkLogin.getApi().getAccountManagement().updateDiscordId(link.username, discordId)) {
                    event.getChannel().sendMessage("¡Tu cuenta de Minecraft **" + link.username + "** ha sido vinculada exitosamente a Discord!").queue();
                    TwoFactorManager.getInstance().removeLinkCode(message);
                } else {
                    event.getChannel().sendMessage("Hubo un error al vincular tu cuenta en la base de datos.").queue();
                }
            } else {
                event.getChannel().sendMessage("Ese código no es válido o ha expirado.").queue();
            }
        } else {
            event.getChannel().sendMessage("Hola. Para vincular tu cuenta, por favor envíame el código de 5 dígitos que te dio el servidor de Minecraft usando `/2fa discord`.").queue();
        }
    }
}
