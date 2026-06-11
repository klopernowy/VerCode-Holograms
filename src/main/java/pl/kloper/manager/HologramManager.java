package pl.kloper.manager;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
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
    private final Map<String, HoloImpl> apiHolograms = new ConcurrentHashMap<>();

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
        config.set("billboard", false);
        config.set("rotatepitch", 0);
        config.set("rotate", 0.0);
        config.set("scale", 1.0);


        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }

        return spawnInternal(cleanedName, config, false);
    }

    public Holo createTemporaryHologramInternal(String name, Location location, Collection<String> lines) {
        String cleanedName = name.toLowerCase();
        removeEntityOnly(cleanedName);

        YamlConfiguration tempConfig = new YamlConfiguration();
        tempConfig.set("location", location);
        tempConfig.set("holograms", new ArrayList<>(lines));
        tempConfig.set("billboard", false);
        tempConfig.set("scale", 1.0);

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
        HoloImpl holo = apiHolograms.get(cleanedName);

        if (holo != null) {
            holo.teleport(newLoc);
            if (!holo.isTemporary()) {
                File file = new File(folder, cleanedName + ".yml");
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                config.set("location", newLoc);
                try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
            }
        } else {
            File file = new File(folder, cleanedName + ".yml");
            if (!file.exists()) return;

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.set("location", newLoc);
            try { config.save(file); } catch (IOException e) { e.printStackTrace(); }

            removeEntityOnly(cleanedName);
            spawn(cleanedName, config);
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

        HoloImpl holo = apiHolograms.remove(cleanedName);
        if (holo != null) {
            holo.deleteRaw();
        }
    }

    public void removeAllEntities() {
        for (String name : new HashSet<>(activeHolograms.keySet())) {
            removeEntityOnly(name);
        }
        for (HoloImpl holo : new HashSet<>(apiHolograms.values())) {
            holo.deleteRaw();
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

    private void spawn(String name, YamlConfiguration config) {
        spawnInternal(name.toLowerCase(), config, false);
    }

    private HoloImpl spawnInternal(String name, YamlConfiguration config, boolean isTemporary) {
        Location loc = config.getLocation("location");
        if (loc == null || loc.getWorld() == null) return null;

        TextDisplay display = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        display.setGravity(false);
        display.setSeeThrough(false);
        display.setShadowed(true);
        display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));

        boolean billboard = config.getBoolean("billboard", false);
        if (billboard) {
            display.setBillboard(Display.Billboard.FIXED);
            float pitch = (float) config.getDouble("rotatepitch", (double) loc.getPitch());
            Location fixedLoc = display.getLocation();
            fixedLoc.setYaw(loc.getYaw());
            fixedLoc.setPitch(pitch);
            display.teleport(fixedLoc);
        } else {
            display.setBillboard(Display.Billboard.CENTER);
        }

        applyTransformations(display, config);

        activeHolograms.put(name, display.getUniqueId());

        HoloImpl holo = new HoloImpl(name, display, config, isTemporary);
        apiHolograms.put(name, holo);

        updateTextInternal(holo);
        return holo;
    }

    private void applyTransformations(TextDisplay display, YamlConfiguration config) {
        float scaleVal = (float) config.getDouble("scale", 1.0);
        float rotateVal = (float) config.getDouble("rotate", 0.0);
        float radians = (float) Math.toRadians(rotateVal);

        Transformation transformation = new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(radians, 0.0f, 1.0f, 0.0f),
                new Vector3f(scaleVal, scaleVal, scaleVal),
                new AxisAngle4f(0, 0, 0, 1)
        );
        display.setTransformation(transformation);
    }

    private void updateText(String name, TextDisplay display, YamlConfiguration config) {
        HoloImpl holo = apiHolograms.get(name.toLowerCase());
        if (holo != null) {
            updateTextInternal(holo);
        } else {
            List<String> rawLines = config.getStringList("holograms");
            if (rawLines.isEmpty()) return;

            List<String> processedLines = new ArrayList<>();
            for (String line : rawLines) {
                String processed = animationManager.processAnimations(name, line);
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    processed = PlaceholderAPI.setPlaceholders(null, processed);
                }
                processedLines.add(ColorUtil.color(processed));
            }
            display.setText(String.join("\n", processedLines));
        }
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
                for (HoloImpl holo : apiHolograms.values()) {
                    if (holo.isTemporary()) {
                        animationManager.updateFrames(holo.getName(), elapsedSeconds);
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        updateTextInternal(holo);
                    });
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20L);
    }

    private void loadAll() {
        File[] files = folder.listFiles((dir, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            spawn(file.getName().replace(".yml", ""), config);
        }
    }

    public List<String> getHologramNames() {
        File[] files = folder.listFiles((dir, n) -> n.endsWith(".yml"));
        if (files == null) return new ArrayList<>();
        return Arrays.stream(files).map(f -> f.getName().replace(".yml", "")).collect(Collectors.toList());
    }

    private class HoloImpl implements Holo {
        private final String name;
        private final TextDisplay display;
        private final YamlConfiguration config;
        private final boolean temporary;
        private BukkitTask followTask;

        public HoloImpl(String name, TextDisplay display, YamlConfiguration config, boolean temporary) {
            this.name = name;
            this.display = display;
            this.config = config;
            this.temporary = temporary;
        }

        public TextDisplay getDisplay() { return display; }
        public YamlConfiguration getConfig() { return config; }
        public boolean isTemporary() { return temporary; }

        @Override public String getName() { return name; }
        @Override public Location getLocation() { return display.getLocation(); }

        @Override
        public void teleport(Location location) {
            display.teleport(location);
            config.set("location", location);
        }

        @Override
        public void setLines(List<String> lines) {
            config.set("holograms", lines);
            updateTextInternal(this);
        }

        @Override
        public List<String> getLines() {
            return config.getStringList("holograms");
        }

        @Override
        public void delete() {
            HologramManager.this.remove(name);
        }

        public void deleteRaw() {
            stopFollowing();
            if (display != null) display.remove();
        }

        @Override
        public void startFollowing(Player player, double heightOffset) {
            stopFollowing();
            this.followTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline() || !display.isValid()) {
                        cancel();
                        delete();
                        return;
                    }
                    display.teleport(player.getLocation().add(0, heightOffset, 0));
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }

        @Override
        public void stopFollowing() {
            if (followTask != null) {
                followTask.cancel();
                followTask = null;
            }
        }
    }
}