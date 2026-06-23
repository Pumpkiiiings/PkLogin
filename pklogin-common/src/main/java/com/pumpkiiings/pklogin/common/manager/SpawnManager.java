package com.pumpkiiings.pklogin.common.manager;

import com.pumpkiiings.pklogin.common.model.Location;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class SpawnManager {

    @Getter
    private final Map<String, Location> spawns = new HashMap<>();

    public void setSpawn(String type, Location location) {
        spawns.put(type.toLowerCase(), location);
    }

    public Location getSpawn(String type) {
        return spawns.get(type.toLowerCase());
    }

    public void load(Map<String, Location> loadedSpawns) {
        spawns.clear();
        spawns.putAll(loadedSpawns);
    }
}
