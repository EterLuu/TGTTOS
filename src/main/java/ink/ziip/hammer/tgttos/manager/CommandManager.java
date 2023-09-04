package ink.ziip.hammer.tgttos.manager;

import ink.ziip.hammer.tgttos.TGTTOS;
import ink.ziip.hammer.tgttos.api.manager.BaseManager;
import ink.ziip.hammer.tgttos.command.MainCommand;
import ink.ziip.hammer.tgttos.command.sub.*;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;

import java.lang.reflect.Field;
import java.util.logging.Level;

public class CommandManager extends BaseManager {

    private MainCommand mainCommand;
    private final PluginCommand corePluginCommand;

    public CommandManager() {
        this.mainCommand = new MainCommand();

        this.corePluginCommand = Bukkit.getPluginCommand("tgttos");
    }

    @Override
    public void load() {
        this.mainCommand = new MainCommand();

        // Sub commands adding
        this.mainCommand.addSubCommand(new JoinCommand());
        this.mainCommand.addSubCommand(new LeaveCommand());
        this.mainCommand.addSubCommand(new AdminCommand());
        this.mainCommand.addSubCommand(new ListCommand());
        this.mainCommand.addSubCommand(new SpectatorCommand());

        if (this.corePluginCommand != null) {
            this.corePluginCommand.setExecutor(this.mainCommand);
            this.corePluginCommand.setTabCompleter(this.mainCommand);
        }
    }

    @Override
    public void unload() {
        Field commandMapField;
        try {
            commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (SimpleCommandMap) commandMapField.get(Bukkit.getServer());

            corePluginCommand.unregister(commandMap);
        } catch (Exception e) {
            TGTTOS.getInstance().getLogger().log(Level.WARNING, "Unregister commands failed.");
        }
    }

    @Override
    public void reload() {

    }
}
