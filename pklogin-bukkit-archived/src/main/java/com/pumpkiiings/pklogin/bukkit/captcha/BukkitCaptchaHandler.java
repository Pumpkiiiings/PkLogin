package com.pumpkiiings.pklogin.bukkit.captcha;

import com.pumpkiiings.pklogin.bukkit.PkLoginBukkit;
import com.pumpkiiings.pklogin.bukkit.ui.title.TitleAPI;
import com.pumpkiiings.pklogin.common.manager.CaptchaManager;
import com.pumpkiiings.pklogin.common.settings.Messages;
import com.pumpkiiings.pklogin.common.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

import java.util.Random;
import java.util.UUID;

public class BukkitCaptchaHandler implements Listener {

    private final PkLoginBukkit plugin;
    private static final Random RANDOM = new Random();

    public BukkitCaptchaHandler(PkLoginBukkit plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static void sendCaptcha(PkLoginBukkit plugin, Player player) {
        String type = Settings.SECURITY_CAPTCHA_TYPE.asString().toUpperCase();
        
        if (type.equals("INVENTORY")) {
            CaptchaManager.getInstance().addPending(player.getName(), ""); // Empty expected code for inventory
            Bukkit.getScheduler().runTask(plugin, () -> openInventoryCaptcha(player));
        } else if (type.equals("CHAT")) {
            String code = generateRandomCode(5);
            CaptchaManager.getInstance().addPending(player.getName(), code);
            player.sendMessage(Messages.CAPTCHA_CHAT_INSTRUCTION.asString().replace("{code}", code));
        } else if (type.equals("MAP")) {
            String code = generateRandomCode(5);
            CaptchaManager.getInstance().addPending(player.getName(), code);
            Bukkit.getScheduler().runTask(plugin, () -> sendMapCaptcha(player, code));
        } else {
            // Fallback
            CaptchaManager.getInstance().addPending(player.getName(), "");
            Bukkit.getScheduler().runTask(plugin, () -> openInventoryCaptcha(player));
        }
    }

    private static void openInventoryCaptcha(Player player) {
        String title = Messages.CAPTCHA_INVENTORY_TITLE.asString();
        // Title might be longer than 32 chars in older versions, but typically safe in newer
        if (title.length() > 32) title = title.substring(0, 32);
        
        Inventory inv = Bukkit.createInventory(null, 27, title);
        
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        if (filler.getType() == Material.AIR) filler = new ItemStack(Material.GLASS); // Fallback for old versions if needed
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_GRAY + "");
            filler.setItemMeta(meta);
        }

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        int randomSlot = RANDOM.nextInt(27);
        ItemStack correctItem = new ItemStack(Material.GREEN_WOOL);
        if (correctItem.getType() == Material.AIR) correctItem = new ItemStack(Material.valueOf("WOOL"), 1, (short) 5);
        
        ItemMeta correctMeta = correctItem.getItemMeta();
        if (correctMeta != null) {
            correctMeta.setDisplayName(Messages.CAPTCHA_INVENTORY_ITEM_NAME.asString());
            correctItem.setItemMeta(correctMeta);
        }
        
        inv.setItem(randomSlot, correctItem);
        player.openInventory(inv);
    }

    private static void sendMapCaptcha(Player player, String code) {
        MapView view = Bukkit.createMap(player.getWorld());
        for (MapRenderer renderer : view.getRenderers()) {
            view.removeRenderer(renderer);
        }
        view.addRenderer(new MapRenderer() {
            @Override
            public void render(MapView map, MapCanvas canvas, Player p) {
                if (!p.getUniqueId().equals(player.getUniqueId())) return;
                canvas.drawText(20, 50, MinecraftFont.Font, "CAPTCHA CODE:");
                canvas.drawText(30, 70, MinecraftFont.Font, "§1" + code);
            }
        });
        
        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        if (mapItem.getType() == Material.AIR) mapItem = new ItemStack(Material.valueOf("MAP"), 1, (short) view.getId());
        else {
            org.bukkit.inventory.meta.MapMeta meta = (org.bukkit.inventory.meta.MapMeta) mapItem.getItemMeta();
            meta.setMapView(view);
            mapItem.setItemMeta(meta);
        }
        
        player.getInventory().setItemInMainHand(mapItem);
        player.sendMessage(Messages.CAPTCHA_MAP_INSTRUCTION.asString());
    }

    private static String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        
        if (CaptchaManager.getInstance().isPending(player.getName())) {
            e.setCancelled(true);
            
            if (e.getCurrentItem() != null) {
                // If they clicked the correct item
                if (e.getCurrentItem().getType().name().contains("GREEN_WOOL") || 
                    (e.getCurrentItem().getType().name().equals("WOOL") && e.getCurrentItem().getDurability() == 5)) {
                    
                    completeCaptcha(player);
                    player.closeInventory();
                } else if (e.getCurrentItem().getType().name().contains("GLASS")) {
                    // Clicked glass pane, ignore
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (CaptchaManager.getInstance().isPending(player.getName())) {
            e.setCancelled(true);
            String type = Settings.SECURITY_CAPTCHA_TYPE.asString().toUpperCase();
            if (type.equals("CHAT") || type.equals("MAP")) {
                String expected = CaptchaManager.getInstance().getExpectedCode(player.getName());
                if (e.getMessage().equalsIgnoreCase(expected)) {
                    Bukkit.getScheduler().runTask(plugin, () -> completeCaptcha(player));
                } else {
                    player.sendMessage(Messages.CAPTCHA_FAILED.asString());
                }
            }
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (CaptchaManager.getInstance().isPending(e.getPlayer().getName())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Messages.CAPTCHA_REQUIRED.asString());
        }
    }

    private void completeCaptcha(Player player) {
        CaptchaManager.getInstance().removePending(player.getName());
        player.sendMessage(Messages.CAPTCHA_SUCCESS.asString());
        
        // Remove map if map captcha was used
        if (Settings.SECURITY_CAPTCHA_TYPE.asString().equalsIgnoreCase("MAP")) {
            player.getInventory().clear();
        }
        
        // Show login/register messages
        boolean registered = plugin.getAccountManagement().retrieveOrLoad(player.getName()).isPresent();
        if (registered) {
            player.sendMessage(Messages.MESSAGE_LOGIN.asString());
            if (Settings.UI_TITLE_BAR.asBoolean()) {
                TitleAPI.getApi().send(player, Messages.TITLE_BEFORE_LOGIN.asTitle());
            }
        } else {
            player.sendMessage(Messages.MESSAGE_REGISTER.asString());
            if (Settings.UI_TITLE_BAR.asBoolean()) {
                TitleAPI.getApi().send(player, Messages.TITLE_BEFORE_REGISTER.asTitle());
            }
        }
    }
}
