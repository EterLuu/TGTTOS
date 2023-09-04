package ink.ziip.hammer.tgttos.command.sub;

import ink.ziip.hammer.tgttos.api.command.BaseSubCommand;
import ink.ziip.hammer.tgttos.api.object.area.Area;
import ink.ziip.hammer.tgttos.api.object.user.PlayerStatusEnum;
import ink.ziip.hammer.tgttos.api.object.user.User;
import ink.ziip.hammer.tgttos.api.util.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class SpectatorCommand extends BaseSubCommand {

    public SpectatorCommand() {
        super("spectator");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            return true;
        }
        User user = User.getUser(sender);
        if (user == null)
            return true;

        if (user.getPlayerStatus() != PlayerStatusEnum.NONE) {
            sender.sendMessage(Utils.translateColorCodes("&c[TGTTOS] &b你已经在旁观/参与一个游戏了，请使用“/tgttos leave”离开。"));
            return true;
        }

        if (Area.getAreaList().contains(args[0])) {
            Area area = Area.getArea(args[0]);
            if (area != null) {
                area.joinAsSpectator(user);
            }
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
