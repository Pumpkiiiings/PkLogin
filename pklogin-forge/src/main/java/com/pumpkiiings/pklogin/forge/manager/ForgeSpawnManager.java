package com.pumpkiiings.pklogin.forge.manager;

import com.pumpkiiings.pklogin.common.manager.SpawnManager;
import com.pumpkiiings.pklogin.common.model.Location;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ForgeSpawnManager extends SpawnManager {

    private final File file;
    private final Yaml yaml;

    public ForgeSpawnManager(File dataFolder) {
        this.file = new File(dataFolder, "spawns.yml");
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        this.yaml = new Yaml(options);
    }

    public void loadSpawns() {
        if (!file.exists()) return;
        try (InputStream inputStream = new FileInputStream(file)) {
            Map<String, Map<String, Object>> data = yaml.load(inputStream);
            if (data == null) return;
            
            Map<String, Location> loaded = new HashMap<>();
            for (Map.Entry<String, Map<String, Object>> entry : data.entrySet()) {
                String type = entry.getKey();
                Map<String, Object> locData = entry.getValue();
                Location loc = new Location(
                        (String) locData.get("world"),
                        ((Number) locData.get("x")).doubleValue(),
                        ((Number) locData.get("y")).doubleValue(),
                        ((Number) locData.get("z")).doubleValue(),
                        ((Number) locData.get("yaw")).floatValue(),
                        ((Number) locData.get("pitch")).floatValue()
                );
                loaded.put(type, loc);
            }
            super.load(loaded);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveSpawns() {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            Map<String, Object> data = new HashMap<>();
            for (Map.Entry<String, Location> entry : getSpawns().entrySet()) {
                Location loc = entry.getValue();
                Map<String, Object> locData = new HashMap<>();
                locData.put("world", loc.getWorld());
                locData.put("x", loc.getX());
                locData.put("y", loc.getY());
                locData.put("z", loc.getZ());
                locData.put("yaw", loc.getYaw());
                locData.put("pitch", loc.getPitch());
                data.put(entry.getKey(), locData);
            }
            try (Writer writer = new FileWriter(file)) {
                yaml.dump(data, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setSpawn(String type, Location location) {
        super.setSpawn(type, location);
        saveSpawns();
    }
}
