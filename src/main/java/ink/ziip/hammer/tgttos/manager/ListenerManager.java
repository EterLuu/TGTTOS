package ink.ziip.hammer.tgttos.manager;

import ink.ziip.hammer.tgttos.TGTTOS;
import ink.ziip.hammer.tgttos.api.listener.BaseListener;
import ink.ziip.hammer.tgttos.api.manager.BaseManager;
import ink.ziip.hammer.tgttos.listener.OtherListener;
import org.bukkit.event.HandlerList;

public class ListenerManager extends BaseManager {

    private final BaseListener otherListener;

    public ListenerManager() {
        otherListener = new OtherListener();
    }

    @Override
    public void load() {

        otherListener.register();

    }

    @Override
    public void unload() {
        HandlerList.unregisterAll(TGTTOS.getInstance());
    }

    @Override
    public void reload() {

    }
}
