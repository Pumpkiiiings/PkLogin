package com.pumpkiiings.pklogin.forge.listener;

import com.pumpkiiings.pklogin.forge.PkLoginForge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ForgeGeneralListeners {

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        String name = player.getGameProfile().getName();

        java.util.Optional<com.pumpkiiings.pklogin.common.model.Account> accountOpt = PkLoginForge.getInstance().getAccountManagement().retrieveOrLoad(name);
        boolean registered = accountOpt.isPresent();

        if (registered) {
            String uuidType = accountOpt.get().getUuidType();
            if (uuidType != null && (uuidType.equals("REAL") || uuidType.equals("PREMIUM"))) {
                // Auto-login!
                PkLoginForge.getInstance().getLoginManagement().setAuthenticated(name);
                player.sendSystemMessage(com.pumpkiiings.pklogin.forge.serializer.chat.ChatComponentSerializer.fromText("§a¡Has iniciado sesión automáticamente vía Premium!"));
                return;
            }
        }

        com.pumpkiiings.pklogin.forge.task.ForgeLoginQueue.addToQueue(name, registered);

        com.pumpkiiings.pklogin.forge.manager.ForgeLimboManager.applyLimboState((ServerPlayer) player);
        if (com.pumpkiiings.pklogin.common.settings.Settings.TELEPORT_SAFE_LOCATION.asBoolean()) {
            com.pumpkiiings.pklogin.forge.manager.ForgeLimboManager.teleportToSpawn((ServerPlayer) player, "join");
        }

        if (registered) {
            player.sendSystemMessage(com.pumpkiiings.pklogin.forge.serializer.chat.ChatComponentSerializer.fromText(com.pumpkiiings.pklogin.common.settings.Messages.MESSAGE_LOGIN.asString("§ePlease login using /login <password>")));
            if (com.pumpkiiings.pklogin.common.settings.Settings.UI_TITLE_BAR.asBoolean()) {
                com.pumpkiiings.pklogin.forge.ui.title.TitleAPI.getApi().send((net.minecraft.server.level.ServerPlayer) player, com.pumpkiiings.pklogin.common.settings.Messages.TITLE_BEFORE_LOGIN.asTitle());
            }
        } else {
            player.sendSystemMessage(com.pumpkiiings.pklogin.forge.serializer.chat.ChatComponentSerializer.fromText(com.pumpkiiings.pklogin.common.settings.Messages.MESSAGE_REGISTER.asString("§ePlease register using /register <password> <password>")));
            if (com.pumpkiiings.pklogin.common.settings.Settings.UI_TITLE_BAR.asBoolean()) {
                com.pumpkiiings.pklogin.forge.ui.title.TitleAPI.getApi().send((net.minecraft.server.level.ServerPlayer) player, com.pumpkiiings.pklogin.common.settings.Messages.TITLE_BEFORE_REGISTER.asTitle());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        String name = player.getGameProfile().getName();
        PkLoginForge.getInstance().getLoginManagement().cleanup(name);
        com.pumpkiiings.pklogin.forge.task.ForgeLoginQueue.removeFromQueue(name);
        if (player instanceof ServerPlayer) {
            com.pumpkiiings.pklogin.forge.manager.ForgeLimboManager.removeLimboState((ServerPlayer) player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onCommandPreprocess(CommandEvent event) {
        if (event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getParseResults().getContext().getSource().getEntity();
            String name = player.getGameProfile().getName();
            
            String command = event.getParseResults().getReader().getString().split(" ")[0].replace("/", "");
            
            if (!PkLoginForge.getInstance().getLoginManagement().isAuthenticated(name)) {
                if (!PkLoginForge.getInstance().getCommandManagement().isAllowedCommand(command)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && event.player instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.player;
            String name = player.getGameProfile().getName();
            if (!PkLoginForge.getInstance().getLoginManagement().isAuthenticated(name)) {
                // Freeze the player
                player.setDeltaMovement(0, 0, 0);
                player.hurtMarked = true; // Forcing position update
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        String name = event.getEntity().getGameProfile().getName();
        if (!PkLoginForge.getInstance().getLoginManagement().isAuthenticated(name)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBlockBreak(PlayerInteractEvent.LeftClickBlock event) {
        String name = event.getEntity().getGameProfile().getName();
        if (!PkLoginForge.getInstance().getLoginManagement().isAuthenticated(name)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onEntityDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            if (!PkLoginForge.getInstance().getLoginManagement().isAuthenticated(player.getGameProfile().getName())) {
                event.setCanceled(true);
                return;
            }
        }
        
        if (event.getSource().getEntity() instanceof ServerPlayer) {
            ServerPlayer damager = (ServerPlayer) event.getSource().getEntity();
            if (!PkLoginForge.getInstance().getLoginManagement().isAuthenticated(damager.getGameProfile().getName())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onServerChat(ServerChatEvent event) {
        String name = event.getPlayer().getGameProfile().getName();
        if (!PkLoginForge.getInstance().getLoginManagement().isAuthenticated(name)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onItemDrop(ItemTossEvent event) {
        String name = event.getPlayer().getGameProfile().getName();
        if (!PkLoginForge.getInstance().getLoginManagement().isAuthenticated(name)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onItemPickup(EntityItemPickupEvent event) {
        String name = event.getEntity().getGameProfile().getName();
        if (!PkLoginForge.getInstance().getLoginManagement().isAuthenticated(name)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onContainerOpen(net.minecraftforge.event.entity.player.PlayerContainerEvent.Open event) {
        String name = event.getEntity().getGameProfile().getName();
        if (!PkLoginForge.getInstance().getLoginManagement().isAuthenticated(name)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        String name = event.getEntity().getGameProfile().getName();
        if (!PkLoginForge.getInstance().getLoginManagement().isAuthenticated(name)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        String name = event.getEntity().getGameProfile().getName();
        if (!PkLoginForge.getInstance().getLoginManagement().isAuthenticated(name)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            com.pumpkiiings.pklogin.forge.manager.ForgeLimboManager.teleportToSpawn(player, "respawn");
        }
    }
}
