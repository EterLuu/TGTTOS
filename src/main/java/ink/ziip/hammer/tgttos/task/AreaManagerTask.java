package ink.ziip.hammer.tgttos.task;

import ink.ziip.hammer.tgttos.api.object.area.Area;
import ink.ziip.hammer.tgttos.api.object.user.PlayerStatusEnum;
import ink.ziip.hammer.tgttos.api.object.user.User;
import ink.ziip.hammer.tgttos.api.task.BaseTask;
import ink.ziip.hammer.tgttos.api.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AreaManagerTask extends BaseTask {

    private static Queue<User> queue = new ConcurrentLinkedQueue<>();

    public AreaManagerTask() {
        super(5);
    }

    @Override
    public void stop() {
        started = false;
        cancel();
    }

    @Override
    public void run() {
        if (started) {
            Area.getAreaList().forEach(area -> {
                Objects.requireNonNull(Area.getArea(area)).checkStart();
            });
            if (!queue.isEmpty()) {
                User user = queue.poll();
                if (user.getArea().checkJoin(user)) {
                    user.getArea().playerJoin(user);
                } else {
                    user.getArea().joinAsSpectator(user);
                }
            }
        }
    }

    public static void playerJoin(User user, Area area) {
        if (user.getPlayerStatus() == PlayerStatusEnum.NONE) {
            user.setArea(area);
            queue.add(user);
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(Utils.translateColorCodes("&c[TGTTOS 调度] &6警告！&c&l选手&f " + user.getPlayer().getName() + " &c&l在旁观游戏！"));
            }
        }
    }
}
