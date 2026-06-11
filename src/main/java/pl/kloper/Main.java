package pl.kloper;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import pl.kloper.api.HologramsAPI;
import pl.kloper.api.HologramsAPIImpl;
import pl.kloper.command.HologramCommand;
import pl.kloper.manager.AnimationManager;
import pl.kloper.manager.HologramManager;

public final class Main extends JavaPlugin {

    private static HologramsAPI api;
    private AnimationManager animationManager;
    private HologramManager hologramManager;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.animationManager = new AnimationManager(this);
        this.hologramManager = new HologramManager(this, this.animationManager);
        api = new HologramsAPIImpl(this.hologramManager);

        if (getCommand("hologram") != null) {
            HologramCommand hologramCommand = new HologramCommand(this.hologramManager);
            getCommand("hologram").setExecutor(hologramCommand);
            getCommand("hologram").setTabCompleter(hologramCommand);
        }

        getLogger().info("Plugin VerCode-Holograms zostal pomyslnie wlaczony!");
    }

    @Override
    public void onDisable() {
        if (this.hologramManager != null) {
            this.hologramManager.removeAllEntities();
        }
        getLogger().info("Plugin VerCode-Holograms zostal wylaczony.");
    }

    public AnimationManager getAnimationManager() {
        return animationManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public static HologramsAPI getAPI() {
        if (api == null) {
            throw new IllegalStateException("API wywolane przed startem pluginu!");
        }
        return api;
    }
}