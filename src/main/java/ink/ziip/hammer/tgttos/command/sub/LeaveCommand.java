package ink.ziip.hammer.tgttos.command.sub;

import ink.ziip.hammer.tgttos.api.command.BaseSubCommand;
import ink.ziip.hammer.tgttos.api.object.user.PlayerStatusEnum;
import ink.ziip.hammer.tgttos.api.object.user.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class LeaveCommand extends BaseSubCommand {

    public LeaveCommand() {
        super("leave");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        User user = User.getUser(sender);

        if (user != null) {
            if (user.getPlayerStatus() != PlayerStatusEnum.NONE) {
                if (user.getArea() != null) {
                    user.getArea().playerQuit(user);
                }
            }
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.singletonList("");
    }
}
