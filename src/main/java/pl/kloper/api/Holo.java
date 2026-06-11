package pl.kloper.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;
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
    Player getTargetPlayer();

    double getCurrentHeightOffset();

    TextDisplay getDisplay();

    void setScale(float scale);
    float getScale();

    void setRotationYaw(float yaw);
    float getRotationYaw();

    void setRotationPitch(float pitch);
    float getRotationPitch();

    void setBillboard(Billboard billboard);
    Billboard getBillboard();
}