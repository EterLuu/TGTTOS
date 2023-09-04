package ink.ziip.hammer.tgttos.listener;

import ink.ziip.hammer.tgttos.api.listener.BaseListener;
import ink.ziip.hammer.tgttos.api.object.user.PlayerStatusEnum;
import ink.ziip.hammer.tgttos.api.object.user.User;
import ink.ziip.hammer.tgttos.manager.ConfigManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class OtherListener extends BaseListener {

    @EventHandler
    public void onFoodLevelChangeEvent(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageEvent event) {
        event.setDamage(0);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        User user = User.getUser(event.getPlayer());
        if (event.getPlayer().getLocation().getY() < -60) {
            if (user.getPlayerStatus() == PlayerStatusEnum.NONE) {
                user.teleport(ConfigManager.spawnLocation);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        User.removeUser(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        User user = User.getUser(event.getPlayer());
        if (!user.getPlayer().hasPermission("tgttos.admin")) {
            user.teleport(ConfigManager.spawnLocation);
        }
    }
}
