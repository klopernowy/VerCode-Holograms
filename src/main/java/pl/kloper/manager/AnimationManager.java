package pl.kloper.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnimationManager {

    private final Plugin plugin;
    private final Map<String, Integer> animationFrames = new ConcurrentHashMap<>();

    public AnimationManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void updateFrames(String hologramName, int elapsedSeconds) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("animations");
        if (section == null) return;

        for (String animKey : section.getKeys(false)) {
            int time = plugin.getConfig().getInt("animations." + animKey + ".time", 1);
            if (time <= 0) time = 1;

            if (elapsedSeconds % time == 0) {
                String stateKey = hologramName + "_" + animKey;
                int currentFrame = animationFrames.getOrDefault(stateKey, 0);
                animationFrames.put(stateKey, currentFrame + 1);
            }
        }
    }

    public String processAnimations(String hologramName, String text) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("animations");
        if (section == null) return text;

        String processed = text;
        for (String animKey : section.getKeys(false)) {
            String placeholder = "%animacja:" + animKey + "%";

            if (processed.contains(placeholder)) {
                int currentFrame = animationFrames.getOrDefault(hologramName + "_" + animKey, 0);
                List<String> swapper = plugin.getConfig().getStringList("animations." + animKey + ".swapper");

                if (!swapper.isEmpty()) {
                    String frameText = swapper.get(currentFrame % swapper.size());
                    processed = processed.replace(placeholder, frameText);
                }
            }
        }
        return processed;
    }

    public void clearFrames() {
        this.animationFrames.clear();
    }
}