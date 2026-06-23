package com.pumpkiiings.pklogin.forge.api.events;

import com.pumpkiiings.pklogin.common.model.Account;
import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

@Getter
public class ForgeAuthenticateEvent extends Event {

    private final ServerPlayer player;
    private final Account account;

    public ForgeAuthenticateEvent(ServerPlayer player, Account account) {
        this.player = player;
        this.account = account;
    }
}
