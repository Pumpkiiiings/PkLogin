package com.pumpkiiings.pkauth.manager;

import com.pumpkiiings.pkauth.PkAuthAddon;
import com.pumpkiiings.pklogin.api.PkLoginAPI;
import com.pumpkiiings.pklogin.common.PkLogin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScoreboardAndTabManager implements Listener {

    private final PkAuthAddon plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();

    public ScoreboardAndTabManager(PkAuthAddon plugin) {
        this.plugin = plugin;
        
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateAll, 20L, 20L);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        setupTablist(player);
        setupScoreboard(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        scoreboards.remove(e.getPlayer().getUniqueId());
    }

    private void setupTablist(Player player) {
        List<String> headerList = plugin.getConfig().getStringList("ui.tablist.header");
        List<String> footerList = plugin.getConfig().getStringList("ui.tablist.footer");
        
        if (headerList.isEmpty()) headerList = List.of("<#FFAA00><b>BIENVENIDO</b>");
        
        String headerStr = String.join("<br>", headerList).replace("%player%", player.getName());
        String footerStr = String.join("<br>", footerList).replace("%player%", player.getName());
        
        player.sendPlayerListHeaderAndFooter(
            colorize(headerStr),
            colorize(footerStr)
        );
    }

    private void setupScoreboard(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("auth_board", Criteria.DUMMY, Component.empty());
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        try {
            obj.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.blank());
        } catch (Throwable ignored) {}
        
        player.setScoreboard(board);
        scoreboards.put(player.getUniqueId(), board);
        
        updateScoreboard(player, board);
    }

    private void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard board = scoreboards.get(player.getUniqueId());
            if (board != null) {
                updateScoreboard(player, board);
            }
        }
    }

    public void setupAllTablists() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            setupTablist(player);
        }
    }

    private void updateScoreboard(Player player, Scoreboard board) {
        boolean isAuthenticated = PkLogin.getApi().isAuthenticated(player.getName());
        String configPath = isAuthenticated ? "ui.scoreboard-authenticated" : "ui.scoreboard";
        
        String titleStr = plugin.getConfig().getString(configPath + ".title", "<#FFAA00><b>PkLogin</b>");
        List<String> lines = plugin.getConfig().getStringList(configPath + ".lines");

        Objective obj = board.getObjective("auth_board");
        if (obj == null) return;

        obj.displayName(colorize(titleStr));
        
        int score = 15;
        for (String line : lines) {
            String parsedLine = line
                .replace("%player%", player.getName())
                .replace("%player_ip%", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "N/A");
                
            String teamName = "line_" + score;
            org.bukkit.scoreboard.Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
            }
            
            // Unique invisible entry
            String entry = org.bukkit.ChatColor.values()[score % 16].toString() + org.bukkit.ChatColor.RESET.toString();
            if (!team.hasEntry(entry)) {
                team.addEntry(entry);
            }
            
            team.prefix(colorize(parsedLine));
            obj.getScore(entry).setScore(score);
            
            score--;
        }
        
        for (int i = 15; i >= 0; i--) {
            if (i <= score) {
                String entry = org.bukkit.ChatColor.values()[i % 16].toString() + org.bukkit.ChatColor.RESET.toString();
                board.resetScores(entry);
            }
        }
    }

    public static Component colorize(String text) {
        if (text == null) return Component.empty();
        
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("[&§]x([&§][a-fA-F0-9]){6}").matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group().replaceAll("[&§x]", "");
            matcher.appendReplacement(sb, "<#" + hex + ">");
        }
        matcher.appendTail(sb);
        text = sb.toString();
        
        matcher = java.util.regex.Pattern.compile("&#([a-fA-F0-9]{6})").matcher(text);
        sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<#" + matcher.group(1) + ">");
        }
        matcher.appendTail(sb);
        text = sb.toString();
        
        text = text.replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>").replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>").replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>").replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>").replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>").replace("&f", "<white>").replace("&k", "<obfuscated>").replace("&l", "<bold>").replace("&m", "<strikethrough>").replace("&n", "<underlined>").replace("&o", "<italic>").replace("&r", "<reset>").replace("§0", "<black>").replace("§1", "<dark_blue>").replace("§2", "<dark_green>").replace("§3", "<dark_aqua>").replace("§4", "<dark_red>").replace("§5", "<dark_purple>").replace("§6", "<gold>").replace("§7", "<gray>").replace("§8", "<dark_gray>").replace("§9", "<blue>").replace("§a", "<green>").replace("§b", "<aqua>").replace("§c", "<red>").replace("§d", "<light_purple>").replace("§e", "<yellow>").replace("§f", "<white>").replace("§k", "<obfuscated>").replace("§l", "<bold>").replace("§m", "<strikethrough>").replace("§n", "<underlined>").replace("§o", "<italic>").replace("§r", "<reset>");
                   
        return MiniMessage.miniMessage().deserialize(text);
    }
}
