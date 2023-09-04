package ink.ziip.hammer.tgttos.api.listener;

import ink.ziip.hammer.tgttos.TGTTOS;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public abstract class BaseListener implements Listener {

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, TGTTOS.getInstance());
    }

    public void unRegister() {
        HandlerList.unregisterAll(this);
    }
}
