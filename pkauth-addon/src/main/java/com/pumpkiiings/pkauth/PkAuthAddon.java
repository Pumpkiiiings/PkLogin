package com.pumpkiiings.pkauth;

import com.pumpkiiings.pkauth.listener.AuthLobbyListener;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import com.pumpkiiings.pkauth.games.snake.listener.protocol.PlayerMovementPacketListener;
import com.pumpkiiings.pkauth.games.snake.listener.PlayerListener;

public class PkAuthAddon extends JavaPlugin {

    private static PkAuthAddon instance;
    private World authWorld;
    private com.pumpkiiings.pkauth.games.rps.RpsGameManager rpsManager;
    private com.pumpkiiings.pkauth.manager.ScoreboardAndTabManager scoreboardManager;

    public static PkAuthAddon getInstance() {
        return instance;
    }

    public com.pumpkiiings.pkauth.games.rps.RpsGameManager getRpsManager() {
        return rpsManager;
    }

    public com.pumpkiiings.pkauth.manager.ScoreboardAndTabManager getScoreboardManager() {
        return scoreboardManager;
    }

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        instance = this;
        PacketEvents.getAPI().init();
        getLogger().info("PkAuthAddon is enabling...");
        
        me.tofaa.entitylib.spigot.SpigotEntityLibPlatform platform = new me.tofaa.entitylib.spigot.SpigotEntityLibPlatform(this);
        me.tofaa.entitylib.APIConfig settings = new me.tofaa.entitylib.APIConfig(PacketEvents.getAPI())
                .trackPlatformEntities()
                .usePlatformLogger();
        me.tofaa.entitylib.EntityLib.init(platform, settings);
        
        saveDefaultConfig();
        
        // Create the empty void world
        authWorld = createVoidWorld("auth");
        
        // Register listeners
        scoreboardManager = new com.pumpkiiings.pkauth.manager.ScoreboardAndTabManager(this);
        getServer().getPluginManager().registerEvents(new AuthLobbyListener(this), this);
        getServer().getPluginManager().registerEvents(scoreboardManager, this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        PacketEvents.getAPI().getEventManager().registerListener(new PlayerMovementPacketListener());

        rpsManager = new com.pumpkiiings.pkauth.games.rps.RpsGameManager(this);
        getServer().getPluginManager().registerEvents(rpsManager, this);
        
        // Register commands
        com.pumpkiiings.pkauth.command.AuthCommandManager.register(this);
        
        getLogger().info("PkAuthAddon has been enabled!");
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
        getLogger().info("PkAuthAddon has been disabled!");
    }

    public World getAuthWorld() {
        return authWorld;
    }

    private World createVoidWorld(String name) {
        WorldCreator creator = new WorldCreator(name);
        creator.generator(new ChunkGenerator() {
            @Override
            public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
                // Return empty chunk data
                return createChunkData(world);
            }
        });
        
        World world = creator.createWorld();
        if (world != null) {
            // Optimize rules for auth world
            world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
            world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(org.bukkit.GameRule.SPAWN_RADIUS, 0);
            world.setTime(6000); // Noon
        }
        return world;
    }
}
