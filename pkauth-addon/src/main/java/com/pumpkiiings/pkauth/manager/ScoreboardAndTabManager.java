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
        String headerStr = plugin.getConfig().getString("ui.tablist.header", "<#FFAA00><b>BIENVENIDO</b>");
        String footerStr = plugin.getConfig().getString("ui.tablist.footer", "");
        
        player.sendPlayerListHeaderAndFooter(
            mm.deserialize(headerStr.replace("%player%", player.getName())),
            mm.deserialize(footerStr)
        );
    }

    private void setupScoreboard(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("auth_board", Criteria.DUMMY, Component.empty());
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        
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

    private void updateScoreboard(Player player, Scoreboard board) {
        boolean isAuthenticated = PkLogin.getApi().isAuthenticated(player.getName());
        String configPath = isAuthenticated ? "ui.scoreboard-authenticated" : "ui.scoreboard";
        
        String titleStr = plugin.getConfig().getString(configPath + ".title", "<#FFAA00><b>PkLogin</b>");
        List<String> lines = plugin.getConfig().getStringList(configPath + ".lines");

        Objective obj = board.getObjective("auth_board");
        if (obj == null) return;

        obj.displayName(mm.deserialize(titleStr));
        
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
            
            team.prefix(mm.deserialize(parsedLine));
            obj.getScore(entry).setScore(score);
            
            score--;
        }
    }
}
