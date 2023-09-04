package ink.ziip.hammer.tgttos;

import ink.ziip.hammer.tgttos.manager.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class TGTTOS extends JavaPlugin {

    private AreaManager areaManager;
    private ConfigManager configManager;
    private TaskManager taskManager;
    private CommandManager commandManager;
    private ListenerManager listenerManager;

    private static TGTTOS instance;

    @Override
    public void onEnable() {
        // Plugin startup logic

        instance = this;

        configManager = new ConfigManager();
        areaManager = new AreaManager();
        taskManager = new TaskManager();
        commandManager = new CommandManager();
        listenerManager = new ListenerManager();

        configManager.load();
        areaManager.load();
        taskManager.load();
        commandManager.load();
        listenerManager.load();

    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic

        configManager.unload();
        areaManager.unload();
        taskManager.unload();
        commandManager.unload();
        listenerManager.unload();
    }

    public static TGTTOS getInstance() {
        return instance;
    }
}
