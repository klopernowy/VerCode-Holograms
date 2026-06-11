package pl.kloper;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import pl.kloper.api.Holo;
import pl.kloper.api.HologramsAPI;
import pl.kloper.api.HologramsAPIImpl;
import pl.kloper.manager.AnimationManager;
import pl.kloper.manager.HologramManager;

public final class Main extends JavaPlugin {

    private HologramManager hologramManager;
    private static HologramsAPI apiInstance;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        apiInstance = new HologramsAPIImpl(this.hologramManager);

        AnimationManager animationManager = new AnimationManager(this);
        this.hologramManager = new HologramManager(this, animationManager);

        long updateInterval = getConfig().getLong("update-interval", 1L);

        getServer().getScheduler().runTaskTimer(this, () -> {
            updateHologramPositions();
        }, 0L, updateInterval);
    }

    @Override
    public void onDisable() {
        if (hologramManager != null) {
            hologramManager.removeAllEntities();
        }
    }

    private void updateHologramPositions() {
        if (hologramManager == null) return;

        for (Holo holo : hologramManager.getActiveHologramsInternal()) {
            Player target = holo.getTargetPlayer();
            TextDisplay display = holo.getDisplay();

            if (target == null || display == null || !display.isValid()) {
                continue;
            }

            if (!target.isOnline()) {
                holo.delete();
                continue;
            }

            Location newLoc = target.getLocation().add(0, holo.getCurrentHeightOffset(), 0);

            display.setInterpolationDuration(1);
            display.setInterpolationDelay(0);

            display.teleport(newLoc);
        }
    }

    public static HologramsAPI getAPI() {
        return apiInstance;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }
}