package pl.kloper.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.List;

public interface Holo {

    String getName();

    Location getLocation();

    void teleport(Location location);

    void setLines(List<String> lines);

    List<String> getLines();

    void delete();

    void startFollowing(Player player, double heightOffset);

    void stopFollowing();
}