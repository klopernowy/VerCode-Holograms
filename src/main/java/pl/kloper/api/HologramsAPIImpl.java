package pl.kloper.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.kloper.manager.HologramManager;

import java.util.Collection;
import java.util.Optional;

public class HologramsAPIImpl implements HologramsAPI {

    private final HologramManager manager;

    public HologramsAPIImpl(HologramManager manager) {
        this.manager = manager;
    }

    @Override
    public Holo createHologram(String name, Location location, Collection<String> lines) {
        return manager.createHologramInternal(name, location, lines);
    }

    @Override
    public Holo createTemporaryHologram(String name, Location location, Collection<String> lines) {
        return manager.createTemporaryHologramInternal(name, location, lines);
    }

    @Override
    public Holo createAbovePlayer(Player player, Collection<String> lines, double heightOffset) {
        return manager.createAbovePlayerInternal(player, lines, heightOffset);
    }

    @Override
    public Optional<Holo> getHologram(String name) {
        return manager.getHologramInternal(name);
    }

    @Override
    public Collection<Holo> getActiveHolograms() {
        return manager.getActiveHologramsInternal();
    }
}