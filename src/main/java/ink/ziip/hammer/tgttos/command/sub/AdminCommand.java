package ink.ziip.hammer.tgttos.command.sub;

import ink.ziip.hammer.teams.api.object.Team;
import ink.ziip.hammer.tgttos.TGTTOS;
import ink.ziip.hammer.tgttos.api.command.BaseSubCommand;
import ink.ziip.hammer.tgttos.api.object.area.Area;
import ink.ziip.hammer.tgttos.api.object.user.User;
import ink.ziip.hammer.tgttos.api.util.Utils;
import ink.ziip.hammer.tgttos.task.AreaManagerTask;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AdminCommand extends BaseSubCommand {

    private final String[] commandList = new String[]{
            "start",
            "end",
            "force-join",
            "force-player"
    };

    public AdminCommand() {
        super("admin");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length <= 1) {
            return true;
        }
        if (args[0].equals("start")) {
            Area area = Area.getArea(args[1]);
            if (area != null) {
                if (area.gameStart()) {
                    sender.sendMessage(Utils.translateColorCodes("&c[TGTTOS] &6强制开启 " + args[1] + " 成功。游戏开始。"));
                } else {
                    sender.sendMessage(Utils.translateColorCodes("&c[TGTTOS] &6强制开启 " + args[1] + " 失败。游戏已经开始。"));
                }
            }
        }

        if (args[0].equals("end")) {
            Area area = Area.getArea(args[1]);
            if (area != null) {
                if (area.gameEnd(false)) {
                    sender.sendMessage(Utils.translateColorCodes("&c[TGTTOS] &6强制关闭 " + args[1] + " 成功。"));
                } else {
                    sender.sendMessage(Utils.translateColorCodes("&c[TGTTOS] &6强制关闭 " + args[1] + " 失败。游戏未开始。"));
                }
            }
        }

        if (args[0].equals("force-join")) {
            Area area = Area.getArea(args[1]);
            if (area != null) {
                Team.getTeamList().forEach(team -> {
                    team.getMemberNames().forEach(name -> {
                        Player player = Bukkit.getPlayer(name);
                        if (player != null) {
                            User user = User.getUser(player);
                            AreaManagerTask.playerJoin(user, area);
                        } else {
                            sender.sendMessage(Utils.translateColorCodes("&c[TGTTOS] &6警告！&c&l玩家&f " + name + " &c&l未在线！"));
                        }
                    });
                });

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.sendMessage(Utils.translateColorCodes("&c[TGTTOS] &6场地&f " + args[1] + " &6即将开始游戏，可以使用命令&f“/tgttos spectator " + args[1] + "”&6旁观。"));
                        }
                    }
                }.runTaskAsynchronously(TGTTOS.getInstance());
            }
        }
        if (args[0].equals("force-player")) {
            Player player = Bukkit.getPlayer(args[2]);
            if (player != null) {
                User user = User.getUser(player);
                Area area = Area.getArea(args[1]);
                if (area != null && user != null) {
                    AreaManagerTask.playerJoin(user, area);
                    sender.sendMessage(Utils.translateColorCodes("&c[TGTTOS] &6成功强制&c&l玩家&f " + args[2] + " &c&l加入！"));
                }
            }
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length <= 1) {
            return Arrays.stream(commandList).toList();
        }
        if ((args[0].equals("start") || args[0].equals("end") || args[0].equals("force-join") || args[0].equals("force-player")) && args.length == 2) {
            List<String> returnList = Area.getAreaList();
            try {
                returnList.removeIf(s -> !s.startsWith(args[1]));
            } catch (Exception ignored) {
            }
            return returnList;
        }
        if (args.length == 3 && args[0].equals("force-player")) {
            List<String> returnList = new java.util.ArrayList<>(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            try {
                returnList.removeIf(s -> !s.startsWith(args[2]));
            } catch (Exception ignored) {
            }
            return returnList;
        }

        return Collections.singletonList("");
    }
}
