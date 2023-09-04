package ink.ziip.hammer.tgttos.command.sub;

import ink.ziip.hammer.tgttos.api.command.BaseSubCommand;
import ink.ziip.hammer.tgttos.api.object.area.Area;
import ink.ziip.hammer.tgttos.api.object.user.User;
import ink.ziip.hammer.tgttos.api.util.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ListCommand extends BaseSubCommand {

    public ListCommand() {
        super("list");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            return true;
        }
        User user = User.getUser(sender);
        if (user == null)
            return true;

        if (Area.getAreaList().contains(args[0])) {
            sender.sendMessage(Utils.translateColorCodes(Area.getArea(args[0]).getPlayerList()));
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length <= 1) {
            List<String> returnList = Area.getAreaList();
            try {
                returnList.removeIf(s -> !s.startsWith(args[0]));
            } catch (Exception ignored) {
            }
            return returnList;
        }

        return Collections.singletonList("");
    }
}
