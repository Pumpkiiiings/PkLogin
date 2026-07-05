package com.pumpkiiings.pkauth.games.rps;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.pumpkiiings.pkauth.PkAuthAddon;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class RpsGameManager implements Listener {

    private final PkAuthAddon plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, UUID> pendingChallenges = new HashMap<>();
    private final Map<UUID, RpsMatch> activeMatches = new HashMap<>();
    private FileConfiguration config;

    public RpsGameManager(PkAuthAddon plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        File file = new File(plugin.getDataFolder(), "rps-menu.yml");
        if (!file.exists()) {
            plugin.saveResource("rps-menu.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void openBotGame(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, mm.deserialize("<#FFAA00><b>Piedra, Papel o Tijeras (vs Bot)</b>"));
        setupMenu(inv);
        player.openInventory(inv);
        activeMatches.put(player.getUniqueId(), new RpsMatch(player.getUniqueId(), null));
    }

    public void challengePlayer(Player challenger, Player target) {
        pendingChallenges.put(target.getUniqueId(), challenger.getUniqueId());
        String msg1 = config.getString("messages.challenge_sent", "<#55FF55>Desafiaste a %target% a RPS.");
        String msg2 = config.getString("messages.challenge_received", "<#FFAA00>%challenger% te ha desafiado a RPS. Usa /auth play rps accept");
        challenger.sendRichMessage(msg1.replace("%target%", target.getName()));
        target.sendRichMessage(msg2.replace("%challenger%", challenger.getName()));
    }

    public void acceptChallenge(Player target) {
        UUID challengerId = pendingChallenges.remove(target.getUniqueId());
        if (challengerId == null) {
            target.sendRichMessage(config.getString("messages.no_challenges", "<#FF5555>No tienes desafíos pendientes."));
            return;
        }

        Player challenger = Bukkit.getPlayer(challengerId);
        if (challenger == null) {
            target.sendRichMessage(config.getString("messages.player_offline", "<#FF5555>El jugador ya no está conectado."));
            return;
        }

        RpsMatch match = new RpsMatch(challengerId, target.getUniqueId());
        activeMatches.put(challengerId, match);
        activeMatches.put(target.getUniqueId(), match);

        Inventory inv1 = Bukkit.createInventory(null, 27, mm.deserialize(config.getString("menu.title", "<#FFAA00><b>Piedra, Papel o Tijeras</b>")));
        setupMenu(inv1);
        challenger.openInventory(inv1);

        Inventory inv2 = Bukkit.createInventory(null, 27, mm.deserialize(config.getString("menu.title", "<#FFAA00><b>Piedra, Papel o Tijeras</b>")));
        setupMenu(inv2);
        target.openInventory(inv2);
    }
    
    public void quitGame(Player player) {
        activeMatches.remove(player.getUniqueId());
        player.closeInventory();
        player.sendRichMessage(config.getString("messages.quit", "<#FF5555>Has salido del juego."));
    }

    private void setupMenu(Inventory inv) {
        inv.setItem(config.getInt("menu.items.rock.slot", 11), createItem(Material.valueOf(config.getString("menu.items.rock.material", "COBBLESTONE")), config.getString("menu.items.rock.name", "<#AAAAAA>Piedra")));
        inv.setItem(config.getInt("menu.items.paper.slot", 13), createItem(Material.valueOf(config.getString("menu.items.paper.material", "PAPER")), config.getString("menu.items.paper.name", "<#FFFFFF>Papel")));
        inv.setItem(config.getInt("menu.items.scissors.slot", 15), createItem(Material.valueOf(config.getString("menu.items.scissors.material", "SHEARS")), config.getString("menu.items.scissors.name", "<#FFAA00>Tijeras")));
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm.deserialize(name));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();

        RpsMatch match = activeMatches.get(player.getUniqueId());
        if (match == null) return;

        if (e.getView().title().equals(mm.deserialize("<#FFAA00><b>Piedra, Papel o Tijeras (vs Bot)</b>")) ||
            e.getView().title().equals(mm.deserialize("<#FFAA00><b>Piedra, Papel o Tijeras</b>"))) {
            
            e.setCancelled(true);
            
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            String choice = "";
            if (clicked.getType() == Material.COBBLESTONE) choice = "PIEDRA";
            else if (clicked.getType() == Material.PAPER) choice = "PAPEL";
            else if (clicked.getType() == Material.SHEARS) choice = "TIJERAS";

            if (!choice.isEmpty()) {
                handleChoice(player, match, choice);
            }
        }
    }

    private void handleChoice(Player player, RpsMatch match, String choice) {
        if (match.opponent == null) {
            String[] choices = {"PIEDRA", "PAPEL", "TIJERAS"};
            String botChoice = choices[new Random().nextInt(choices.length)];
            
            String result = getResult(choice, botChoice);
            String msg = config.getString("messages.vs_bot_result", "<#AAAAAA>Bot eligió: %bot_choice%. <#FFFFFF>¡%result%!");
            player.sendRichMessage(msg.replace("%bot_choice%", botChoice).replace("%result%", result));
            player.closeInventory();
            activeMatches.remove(player.getUniqueId());
        } else {
            match.setChoice(player.getUniqueId(), choice);
            String waitMsg = config.getString("messages.waiting_opponent", "<#55FF55>Has elegido %choice%. Esperando al rival...");
            player.sendRichMessage(waitMsg.replace("%choice%", choice));
            player.closeInventory();

            if (match.isReady()) {
                Player p1 = Bukkit.getPlayer(match.player1);
                Player p2 = Bukkit.getPlayer(match.opponent);

                if (p1 != null && p2 != null) {
                    String resultP1 = getResult(match.choice1, match.choice2);
                    String resultP2 = getResult(match.choice2, match.choice1);

                    String msgBase = config.getString("messages.vs_player_result", "<#AAAAAA>%opponent% eligió %opponent_choice%. <#FFFFFF>¡%result%!");
                    
                    p1.sendRichMessage(msgBase.replace("%opponent%", p2.getName()).replace("%opponent_choice%", match.choice2).replace("%result%", resultP1));
                    p2.sendRichMessage(msgBase.replace("%opponent%", p1.getName()).replace("%opponent_choice%", match.choice1).replace("%result%", resultP2));
                }
                
                activeMatches.remove(match.player1);
                activeMatches.remove(match.opponent);
            }
        }
    }

    private String getResult(String p1, String p2) {
        if (p1.equals(p2)) return "Empate";
        if ((p1.equals("PIEDRA") && p2.equals("TIJERAS")) ||
            (p1.equals("PAPEL") && p2.equals("PIEDRA")) ||
            (p1.equals("TIJERAS") && p2.equals("PAPEL"))) {
            return "Ganaste";
        }
        return "Perdiste";
    }

    private static class RpsMatch {
        UUID player1;
        UUID opponent;
        String choice1;
        String choice2;

        public RpsMatch(UUID p1, UUID p2) {
            this.player1 = p1;
            this.opponent = p2;
        }

        public void setChoice(UUID player, String choice) {
            if (player.equals(player1)) choice1 = choice;
            else if (player.equals(opponent)) choice2 = choice;
        }

        public boolean isReady() {
            return choice1 != null && choice2 != null;
        }
    }
}
