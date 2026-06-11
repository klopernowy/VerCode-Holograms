package pl.kloper.manager;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import pl.kloper.api.Holo;
import pl.kloper.utils.ColorUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class HologramManager {

    private final Plugin plugin;
    private final File folder;
    private final AnimationManager animationManager;
    private final Map<String, UUID> activeHolograms = new ConcurrentHashMap<>();
    private final Map<String, Holo> apiHolograms = new ConcurrentHashMap<>();

    public HologramManager(Plugin plugin, AnimationManager animationManager) {
        this.plugin = plugin;
        this.animationManager = animationManager;
        this.folder = new File(plugin.getDataFolder(), "holograms");
        if (!folder.exists()) folder.mkdirs();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            loadAll();
            startUpdateTasks();
        }, 20L);
    }

    public void create(String name, Location loc, String text) {
        createHologramInternal(name.toLowerCase(), loc, Collections.singletonList(text));
    }

    public Holo createHologramInternal(String name, Location location, Collection<String> lines) {
        String cleanedName = name.toLowerCase();
        removeEntityOnly(cleanedName);

        File file = new File(folder, cleanedName + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("location", location);
        config.set("holograms", new ArrayList<>(lines));
        config.set("billboard", "CENTER");
        config.set("rotatepitch", 0.0f);
        config.set("rotate", 0.0f);
        config.set("scale", 1.0f);

        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }

        return spawnInternal(cleanedName, config, false);
    }

    public Holo createTemporaryHologramInternal(String name, Location location, Collection<String> lines) {
        String cleanedName = name.toLowerCase();
        removeEntityOnly(cleanedName);

        YamlConfiguration tempConfig = new YamlConfiguration();
        tempConfig.set("location", location);
        tempConfig.set("holograms", new ArrayList<>(lines));
        tempConfig.set("billboard", "CENTER");
        tempConfig.set("rotatepitch", 0.0f);
        tempConfig.set("rotate", 0.0f);
        tempConfig.set("scale", 1.0f);

        return spawnInternal(cleanedName, tempConfig, true);
    }

    public Holo createAbovePlayerInternal(Player player, Collection<String> lines, double heightOffset) {
        String uniqueName = "player_" + player.getName() + "_" + System.currentTimeMillis();
        Location loc = player.getLocation().add(0, heightOffset, 0);

        Holo holo = createTemporaryHologramInternal(uniqueName, loc, lines);
        holo.startFollowing(player, heightOffset);
        return holo;
    }

    public Optional<Holo> getHologramInternal(String name) {
        return Optional.ofNullable(apiHolograms.get(name.toLowerCase()));
    }

    public Collection<Holo> getActiveHologramsInternal() {
        return new ArrayList<>(apiHolograms.values());
    }

    public void moveHere(String name, Location newLoc) {
        String cleanedName = name.toLowerCase();
        Holo holo = apiHolograms.get(cleanedName);
        if (holo != null) {
            holo.teleport(newLoc);
        }
    }

    public void remove(String name) {
        String cleanedName = name.toLowerCase();
        removeEntityOnly(cleanedName);

        File file = new File(folder, cleanedName + ".yml");
        if (file.exists()) file.delete();
    }

    public void removeEntityOnly(String name) {
        String cleanedName = name.toLowerCase();

        UUID uuid = activeHolograms.remove(cleanedName);
        if (uuid != null) {
            var entity = plugin.getServer().getEntity(uuid);
            if (entity != null) entity.remove();
        }

        Holo holo = apiHolograms.remove(cleanedName);
        if (holo != null && holo instanceof HoloImpl) {
            ((HoloImpl) holo).deleteRaw();
        }
    }

    public void removeAllEntities() {
        for (String name : new HashSet<>(activeHolograms.keySet())) {
            removeEntityOnly(name);
        }
        activeHolograms.clear();
        apiHolograms.clear();
    }

    public void reloadAll() {
        removeAllEntities();
        animationManager.clearFrames();
        plugin.reloadConfig();
        loadAll();
    }

    private void loadAll() {
        File[] files = folder.listFiles((dir, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            spawnInternal(file.getName().replace(".yml", "").toLowerCase(), config, false);
        }
    }

    private HoloImpl spawnInternal(String name, YamlConfiguration config, boolean isTemporary) {
        Location loc = config.getLocation("location");
        if (loc == null || loc.getWorld() == null) return null;

        TextDisplay display = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        display.setGravity(false);
        display.setSeeThrough(false);
        display.setShadowed(true);
        display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));

        String billboardType = config.getString("billboard", "CENTER");
        try {
            display.setBillboard(Billboard.valueOf(billboardType.toUpperCase()));
        } catch (IllegalArgumentException e) {
            display.setBillboard(Billboard.CENTER);
        }

        activeHolograms.put(name, display.getUniqueId());

        HoloImpl holo = new HoloImpl(name, display, config, isTemporary);
        apiHolograms.put(name, holo);

        holo.updateTransformations();
        updateTextInternal(holo);

        return holo;
    }

    private void updateTextInternal(HoloImpl holo) {
        TextDisplay display = holo.getDisplay();
        if (display == null || !display.isValid()) return;

        List<String> rawLines = holo.getLines();
        if (rawLines.isEmpty()) return;

        List<String> processedLines = new ArrayList<>();
        for (String line : rawLines) {
            String processed = animationManager.processAnimations(holo.getName(), line);
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                processed = PlaceholderAPI.setPlaceholders(null, processed);
            }
            processedLines.add(ColorUtil.color(processed));
        }
        display.setText(String.join("\n", processedLines));
    }

    private void startUpdateTasks() {
        new BukkitRunnable() {
            private int elapsedSeconds = 0;

            @Override
            public void run() {
                elapsedSeconds++;

                for (String name : activeHolograms.keySet()) {
                    animationManager.updateFrames(name, elapsedSeconds);
                }
                for (Holo holo : apiHolograms.values()) {
                    if (holo instanceof HoloImpl) {
                        HoloImpl holoImpl = (HoloImpl) holo;
                        if (holoImpl.isTemporary()) {
                            animationManager.updateFrames(holoImpl.getName(), elapsedSeconds);
                        }
                        Bukkit.getScheduler().runTask(plugin, () -> updateTextInternal(holoImpl));
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20L);
    }

    public List<String> getHologramNames() {
        File[] files = folder.listFiles((dir, n) -> n.endsWith(".yml"));
        if (files == null) return new ArrayList<>();
        return Arrays.stream(files).map(f -> f.getName().replace(".yml", "")).collect(Collectors.toList());
    }

    public class HoloImpl implements Holo {
        private final String name;
        private final TextDisplay display;
        private final YamlConfiguration config;
        private final boolean temporary;

        private Player targetPlayer;
        private double currentHeightOffset;

        public HoloImpl(String name, TextDisplay display, YamlConfiguration config, boolean temporary) {
            this.name = name;
            this.display = display;
            this.config = config;
            this.temporary = temporary;
        }

        public TextDisplay getDisplay() { return display; }
        public boolean isTemporary() { return temporary; }

        @Override public String getName() { return name; }
        @Override public Location getLocation() { return display.getLocation(); }

        @Override
        public void teleport(Location location) {
            display.teleport(location);
            config.set("location", location);
            saveConfig();
        }

        @Override
        public void setLines(List<String> lines) {
            config.set("holograms", lines);
            saveConfig();
            updateTextInternal(this);
        }

        @Override public List<String> getLines() { return config.getStringList("holograms"); }
        @Override public void delete() { HologramManager.this.remove(name); }

        public void deleteRaw() {
            stopFollowing();
            if (display != null) display.remove();
        }

        @Override
        public void startFollowing(Player player, double heightOffset) {
            this.targetPlayer = player;
            this.currentHeightOffset = heightOffset;
        }

        @Override public void stopFollowing() { this.targetPlayer = null; }
        @Override public Player getTargetPlayer() { return targetPlayer; }
        @Override public double getCurrentHeightOffset() { return currentHeightOffset; }

        // --- SKALOWANIE I ROTACJA ---

        @Override
        public void setScale(float scale) {
            config.set("scale", scale);
            saveConfig();
            updateTransformations();
        }
        @Override public float getScale() { return (float) config.getDouble("scale", 1.0); }

        @Override
        public void setRotationYaw(float yaw) {
            config.set("rotate", yaw);
            saveConfig();
            updateTransformations();
        }
        @Override public float getRotationYaw() { return (float) config.getDouble("rotate", 0.0); }

        @Override
        public void setRotationPitch(float pitch) {
            config.set("rotatepitch", pitch);
            saveConfig();
            updateTransformations();
        }
        @Override public float getRotationPitch() { return (float) config.getDouble("rotatepitch", 0.0); }

        @Override
        public void setBillboard(Billboard billboard) {
            config.set("billboard", billboard.name());
            saveConfig();
            if (display != null) display.setBillboard(billboard);
        }
        @Override
        public Billboard getBillboard() {
            return display != null ? display.getBillboard() : Billboard.CENTER;
        }

        public void updateTransformations() {
            if (display == null) return;

            float scaleVal = getScale();
            float yawRadians = (float) Math.toRadians(getRotationYaw());
            float pitchRadians = (float) Math.toRadians(getRotationPitch());

            org.joml.Quaternionf mainRotation = new org.joml.Quaternionf()
                    .rotationY(yawRadians)
                    .rotateX(pitchRadians);

            org.joml.Quaternionf identityRotation = new org.joml.Quaternionf();

            org.bukkit.util.Transformation transformation = new org.bukkit.util.Transformation(
                    new org.joml.Vector3f(0.0f, 0.0f, 0.0f),
                    mainRotation,
                    new org.joml.Vector3f(scaleVal, scaleVal, scaleVal),
                    identityRotation
            );

            display.setTransformation(transformation);
        }

        private void saveConfig() {
            if (temporary) return;
            try {
                config.save(new File(folder, name + ".yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}