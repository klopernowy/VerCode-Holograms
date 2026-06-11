package pl.kloper.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.Collection;
import java.util.Optional;

public interface HologramsAPI {

    Holo createHologram(String name, Location location, Collection<String> lines);

    Holo createTemporaryHologram(String name, Location location, Collection<String> lines);

    Holo createAbovePlayer(Player player, Collection<String> lines, double heightOffset);

    Optional<Holo> getHologram(String name);

    Collection<Holo> getActiveHolograms();
}