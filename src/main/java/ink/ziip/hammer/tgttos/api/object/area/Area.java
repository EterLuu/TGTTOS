package ink.ziip.hammer.tgttos.api.object.area;

import ink.ziip.hammer.teams.api.object.GameTypeEnum;
import ink.ziip.hammer.teams.api.object.Team;
import ink.ziip.hammer.teams.manager.TeamRecordManager;
import ink.ziip.hammer.tgttos.TGTTOS;
import ink.ziip.hammer.tgttos.api.object.user.PlayerStatusEnum;
import ink.ziip.hammer.tgttos.api.object.user.User;
import ink.ziip.hammer.tgttos.api.util.Utils;
import ink.ziip.hammer.tgttos.manager.ConfigManager;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class Area implements Listener {

    private static final Map<String, Area> areas = new ConcurrentHashMap<>();

    private String areaName;
    private int areaMinPlayer;
    private int areaMaxPlayer;
    private AreaTypeEnum areaType;
    private int areaTimer;
    private int areaDefaultTimer;
    private Location areaSpawnPoint;
    private final List<Location> areaSpawners = new ArrayList<>();
    private Location areaPos1;
    private Location areaPos2;
    private BoundingBox areaBoundingBox;
    private Location areaChickenSpawnPoint;
    private final List<Location> areaSpawnPoints = new ArrayList<>();

    private World world;
    private Boolean status = false;
    private Boolean started = false;
    private Long gameStartTime;

    private final Map<UUID, User> users = new ConcurrentHashMap<>();
    private final Map<User, Long> playerTimeToEnd = new ConcurrentHashMap<>();
    private final List<BlockState> blockStates = new ArrayList<>();

    private int runTaskId;

    public void loadFromConfig(String name) {
        File file = new File(TGTTOS.getInstance().getDataFolder() + File.separator + "areas" + File.separator + name + ".yml");

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        areaName = config.getString("area.name");
        areaMinPlayer = config.getInt("area.min-player");
        areaMaxPlayer = config.getInt("area.max-player");
        areaType = AreaTypeEnum.valueOf(config.getString("area.area-type"));
        areaTimer = 0;
        areaDefaultTimer = config.getInt("area.timer");
        areaSpawnPoint = Utils.getLocation(config.getString("area.spawn-point"));
        config.getStringList("area.spawners").forEach(content -> {
            areaSpawners.add(Utils.getLocation(content));
        });
        areaPos1 = Utils.getLocation(config.getString("area.pos1"));
        areaPos2 = Utils.getLocation(config.getString("area.pos2"));
        areaBoundingBox = new BoundingBox(
                areaPos1.getX(), areaPos1.getY(), areaPos1.getZ(),
                areaPos2.getX(), areaPos2.getY(), areaPos2.getZ());

        areaChickenSpawnPoint = Utils.getLocation(config.getString("area.end-chicken-spawn-point"));

        config.getStringList("area.spawn-points").forEach(content -> {
            areaSpawnPoints.add(Utils.getLocation(content));
        });

        world = areaPos1.getWorld();

        regenArea();

        status = true;
        Bukkit.getPluginManager().registerEvents(this, TGTTOS.getInstance());
    }

    public static Area getArea(String name) {
        if (areas.containsKey(name)) {
            return areas.get(name);
        }
        return null;
    }

    public static void createArea(String name) {
        if (!areas.containsKey(name)) {
            Area area = new Area();
            area.loadFromConfig(name);
            areas.put(name, area);
        }
    }

    public static List<String> getAreaList() {
        return new ArrayList<>(areas.keySet().stream().toList());
    }

    public String getPlayerList() {
        ArrayList<Map.Entry<UUID, User>> list = new ArrayList<>(users.entrySet());

        StringBuilder playerList = new StringBuilder();

        playerList.append("&b========= &c").append(areaName).append("&b =========\n");

        for (Map.Entry<UUID, User> entry : list) {
            playerList.append("&6").append(entry.getValue().getPlayer().getName())
                    .append("&c：&f")
                    .append(entry.getValue().getPlayerStatus().toString())
            ;
        }

        return playerList.toString();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPlaceBlock(BlockPlaceEvent event) {
        if (!status || !started) {
            return;
        }
        if (!users.containsKey(event.getPlayer().getUniqueId()))
            return;

        Location location = event.getBlock().getLocation();
        if (areaBoundingBox.contains(location.getX(), location.getY(), location.getZ())) {

            if (areaType != AreaTypeEnum.ROAD || areaTimer > areaDefaultTimer) {
                event.setCancelled(true);
                return;
            }
            blockStates.add(event.getBlockPlaced().getState());
            event.getItemInHand().setAmount(64);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBreakBlock(BlockBreakEvent event) {
        if (!status || !started) {
            return;
        }
        if (!users.containsKey(event.getPlayer().getUniqueId()))
            return;

        Location location = event.getBlock().getLocation();
        if (areaBoundingBox.contains(location.getX(), location.getY(), location.getZ())) {

            if (!blockStates.contains(event.getBlock().getState()) || areaTimer > areaDefaultTimer) {
                event.setCancelled(true);
            }

            event.setDropItems(false);
        }
    }

    public void regenArea() {
        status = false;

        new BukkitRunnable() {

            @Override
            public void run() {
                regenAreaNow();
            }
        }.runTaskLater(TGTTOS.getInstance(), 0L);
    }

    public void regenAreaNow() {
        status = false;
        if (areaType == AreaTypeEnum.ROAD) {
            for (BlockState blockState : blockStates) {
                blockState.setType(Material.AIR);
                blockState.update(true);
            }
        }
        if (areaType == AreaTypeEnum.BOAT) {
            for (Entity entity : world.getNearbyEntities(areaBoundingBox)) {
                if (entity instanceof Boat) {
                    entity.remove();
                }
            }
        }
        if (!areaSpawners.isEmpty()) {
            for (Entity entity : world.getNearbyEntities(areaBoundingBox)) {
                if (entity instanceof Stray) {
                    entity.remove();
                }
            }
        }
        for (Entity entity : world.getNearbyEntities(areaBoundingBox)) {
            if (entity instanceof Chicken) {
                entity.remove();
            }
        }
        resetData();
        status = true;
    }

    private void resetData() {
        areaTimer = 0;
        users.clear();
        gameStartTime = 0L;
        blockStates.clear();
        playerTimeToEnd.clear();
        started = false;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!status || !started) {
            return;
        }
        if (!users.containsKey(event.getEntity().getUniqueId()))
            return;

        Location location = event.getEntity().getLocation();
        if (areaBoundingBox.contains(location.getX(), location.getY(), location.getZ())) {

            if (areaTimer > areaDefaultTimer) {
                event.setCancelled(true);
                return;
            }
            event.setDamage(0);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDamageEntity(EntityDamageByEntityEvent event) {
        if (!status || !started) {
            return;
        }
        if (!users.containsKey(event.getDamager().getUniqueId()))
            return;

        Location location = event.getEntity().getLocation();
        if (areaBoundingBox.contains(location.getX(), location.getY(), location.getZ())) {
            if (event.getDamager() instanceof Player && event.getEntity() instanceof Chicken) {
                User user = User.getUser(event.getDamager());

                if (user != null && user.getPlayerStatus() == PlayerStatusEnum.PLAYER) {
                    event.getEntity().remove();

                    // Arrived at end point

                    arrivedAtEndPoint(user);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!status) {
            return;
        }
        if (!users.containsKey(event.getPlayer().getUniqueId()))
            return;

        Location location = event.getPlayer().getLocation();
        if (areaBoundingBox.contains(location.getX(), location.getY(), location.getZ())) {
            if (users.get(event.getPlayer().getUniqueId()).getPlayerStatus() == PlayerStatusEnum.SPECTATOR)
                return;
            if (areaTimer >= areaDefaultTimer) {
                event.setCancelled(true);
            }
        } else {
            teleportUserToSpawnPoint(User.getUser(event.getPlayer()));
            if (started) {
                if (areaType == AreaTypeEnum.BOAT) {
                    giveBoatToUser(User.getUser(event.getPlayer()));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteraction(PlayerInteractEvent event) {
        if (!status) {
            return;
        }
        if (!users.containsKey(event.getPlayer().getUniqueId()))
            return;

        Location location = event.getPlayer().getLocation();
        if (areaBoundingBox.contains(location.getX(), location.getY(), location.getZ())) {
            if (users.get(event.getPlayer().getUniqueId()).getPlayerStatus() == PlayerStatusEnum.SPECTATOR)
                return;
            if (areaTimer >= areaDefaultTimer || !started) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLeave(PlayerQuitEvent event) {
        if (!users.containsKey(event.getPlayer().getUniqueId()))
            return;

        if (users.get(event.getPlayer().getUniqueId()).getPlayerStatus() == PlayerStatusEnum.PLAYER) {
            playerQuit(users.get(event.getPlayer().getUniqueId()));
        } else {
            endGameForUser(users.get(event.getPlayer().getUniqueId()), false);
        }
    }

    public void playerJoin(User user) {
        if (checkJoin(user)) {
            user.joinAreaAsPlayer(this);
            users.put(user.getPlayerUUID(), user);
            teleportUserToSpawnPoint(user);
            user.setGameMode(GameMode.ADVENTURE);
            user.getPlayer().getInventory().clear();

            announceToAllUsers("&c[TGTTOS] &b玩家 " + user.getPlayer().getName() + " 加入了游戏。");
        }
    }

    public boolean checkJoin(User user) {
        if (status) {
            if (!started) {
                if (users.size() < areaMaxPlayer) {
                    return user.getPlayerStatus() == PlayerStatusEnum.NONE;
                }
            }
        }
        return false;
    }

    public void playerQuit(User user) {
        endGameForUser(user, true);
    }

    public void joinAsSpectator(User user) {
        if (user.getPlayerStatus() == PlayerStatusEnum.NONE) {
            user.joinAreaAsSpectator(this);
            users.put(user.getPlayerUUID(), user);
            teleportUserToSpawnPoint(user);
            user.setGameMode(GameMode.SPECTATOR);
        }
    }

    public void arrivedAtEndPoint(User user) {
        user.setPlayerStatus(PlayerStatusEnum.SPECTATOR);
        Long time = System.currentTimeMillis() - gameStartTime;
        playerTimeToEnd.put(user, time);
        String content = "&c[TGTTOS] &b玩家 " + user.getPlayer().getName() + " 到达了终点！&f（耗时：" + time + "）";
        Bukkit.getLogger().log(Level.INFO, areaName + " " + content);
        announceToAllUsers(content);
        user.setGameMode(GameMode.SPECTATOR);
    }

    public void checkStart() {
        if (status) {
            if (!started) {
                int count = 0;
                for (User user : users.values()) {
                    if (user.getPlayerStatus() == PlayerStatusEnum.PLAYER) {
                        count++;
                    }
                }
                sendActionBarToAllUsers("&6还需要&f " + String.valueOf(areaMaxPlayer - count) + " &6个玩家。");
                if (count == areaMaxPlayer) {
                    gameStart();
                }
            }
        }
    }

    public boolean gameStart() {

        if (started)
            return false;

        started = true;

        areaTimer = areaDefaultTimer + 10;

        if (areaType == AreaTypeEnum.BOAT) {
            giveBoatToAllPlayers();
        }
        if (areaType == AreaTypeEnum.ROAD) {
            giveRoadToolsToAllPlayers();
        }
        if (!areaSpawners.isEmpty()) {
            spawnMonsters();
        }

        spawnChicken();

        teleportAllPlayerToSpawnPoints();

        if (areaType == AreaTypeEnum.ROAD) {
            setGameModeForAllPlayers(GameMode.SURVIVAL);
        }

        gameStartTime = System.currentTimeMillis();

        runTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(TGTTOS.getInstance(), new Runnable() {
            @Override
            public void run() {

                setAllPlayerLevel(areaTimer);
                sendActionBarToAllSpectators(" &6游戏剩余时间：&c" + areaTimer);

                if (areaTimer > areaDefaultTimer) {
                    sendTitleToAllUsers("&b游戏即将开始", "&c倒计时：&6" + String.valueOf(areaTimer - areaDefaultTimer));
                    playerSoundToAllUsers(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 0.5F);
                }

                if (Objects.equals(areaTimer, areaDefaultTimer)) {
                    sendTitleToAllUsers("&c[TGTTOS]", "&b游戏开始！");
                    playerSoundToAllUsers(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1F);
                }

                if (areaTimer == 5) {
                    playerSoundToAllUsers(Sound.ENTITY_PLAYER_LEVELUP, 1, 0.5F);
                } else if (areaTimer == 4) {
                    playerSoundToAllUsers(Sound.ENTITY_PLAYER_LEVELUP, 1, 0.6F);
                } else if (areaTimer == 3) {
                    playerSoundToAllUsers(Sound.ENTITY_PLAYER_LEVELUP, 1, 0.7F);
                } else if (areaTimer == 2) {
                    playerSoundToAllUsers(Sound.ENTITY_PLAYER_LEVELUP, 1, 0.8F);
                } else if (areaTimer == 1) {
                    playerSoundToAllUsers(Sound.ENTITY_PLAYER_LEVELUP, 1, 0.9F);
                }

                if (areaTimer == 0) {
                    gameEnd(false);
                    return;
                }

                areaTimer = areaTimer - 1;
            }
        }, 0L, 20L);

        return true;
    }

    public boolean gameEnd(boolean unload) {

        if (!started) {
            return false;
        }

        Bukkit.getScheduler().cancelTask(runTaskId);

        endGameForUsers();

        if (unload) {
            regenAreaNow();
        } else {
            regenArea();
        }

        return true;
    }

    private void endGameForUsers() {
        ArrayList<Map.Entry<User, Long>> list = new ArrayList<>(playerTimeToEnd.entrySet());
        list.sort(Map.Entry.comparingByValue());

        StringBuilder rank = new StringBuilder();

        rank.append("&b========= &c").append(areaName).append("&b =========\n");

        int i = 1;

        for (Map.Entry<User, Long> entry : list) {
            int points = 51 - i;
            if (i <= 10) {
                points += 100 - 5 * (i - 1);
            }

            TeamRecordManager.addPlayerRecord(entry.getKey().getPlayer().getName(), Team.getTeamByPlayer(entry.getKey().getPlayer()), GameTypeEnum.TGTTOS.name(), areaName, points, false);

            rank.append("\n&6").append(i).append(". &f")
                    .append(entry.getKey().getPlayer().getName())
                    .append("&c：&f")
                    .append(Duration.ofMillis(entry.getValue()).toMinutesPart())
                    .append("分")
                    .append(Duration.ofMillis(entry.getValue()).toSecondsPart())
                    .append("秒")
                    .append(Duration.ofMillis(entry.getValue()).toMillisPart())
                    .append("毫秒").append("&f，获得积分 ").append(points);
            ;
            i++;
        }

        announceToAllUsers("&c[TGTTOS] &b游戏结束。");
        announceToAllUsers(rank.toString());
        Bukkit.getLogger().log(Level.INFO, rank.toString());

        for (Map.Entry<UUID, User> userEntry : users.entrySet()) {
            endGameForUser(userEntry.getValue(), false);
        }
    }

    private void endGameForUser(User user, boolean announce) {
        users.remove(user.getPlayerUUID());
        user.leaveArea(this);
        user.setLevel(0);
        user.getPlayer().getInventory().clear();
        user.teleport(ConfigManager.spawnLocation);
        user.setGameMode(GameMode.ADVENTURE);
        if (announce)
            if (user.getPlayerStatus() == PlayerStatusEnum.PLAYER)
                announceToAllUsers("&c[TGTTOS] &b玩家 " + user.getPlayer().getName() + " 退出了游戏。");
    }

    private void teleportUserToSpawnPoint(User user) {
        user.teleport(areaSpawnPoint);
        user.sendMessage("&c[TGTTOS] &b将你传送至出生点。");
        user.sendTitle("&c[TGTTOS]", "&b传送至出生点");
    }

    private void setGameModeForAllPlayers(GameMode gameMode) {
        for (Map.Entry<UUID, User> userEntry : users.entrySet()) {
            if (userEntry.getValue().getPlayerStatus() == PlayerStatusEnum.PLAYER) {
                userEntry.getValue().setGameMode(gameMode);
            }
        }
    }

    private void announceToAllUsers(String content) {
        for (Map.Entry<UUID, User> userEntry : users.entrySet()) {
            userEntry.getValue().sendMessage(content);
        }
        Bukkit.getLogger().log(Level.INFO, areaName + " " + content);
    }

    private void sendTitleToAllUsers(String title, String subTitle) {
        for (Map.Entry<UUID, User> userEntry : users.entrySet()) {
            userEntry.getValue().sendTitle(title, subTitle);
        }
    }

    private void sendActionBarToAllSpectators(String content) {
        for (Map.Entry<UUID, User> userEntry : users.entrySet()) {
            if (userEntry.getValue().getPlayerStatus() == PlayerStatusEnum.SPECTATOR)
                userEntry.getValue().sendActionBar(content, false);
        }
    }

    private void sendActionBarToAllUsers(String content) {
        for (Map.Entry<UUID, User> userEntry : users.entrySet()) {
            userEntry.getValue().sendActionBar(content, false);
        }
    }

    private void setAllPlayerLevel(int level) {
        for (Map.Entry<UUID, User> userEntry : users.entrySet()) {
            if (userEntry.getValue().getPlayerStatus() == PlayerStatusEnum.PLAYER) {
                userEntry.getValue().setLevel(level);
            }
        }
    }

    private void playerSoundToAllUsers(Sound sound, float volume, float pitch) {
        for (Map.Entry<UUID, User> userEntry : users.entrySet()) {
            userEntry.getValue().playSound(sound, volume, pitch);
        }
    }

    private void giveBoatToUser(User user) {
        if (user.getPlayerStatus() == PlayerStatusEnum.PLAYER) {
            ItemStack itemStack = new ItemStack(Material.OAK_BOAT);
            user.getPlayer().getInventory().addItem(itemStack);
        }
    }

    private void spawnMonsters() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Location location : areaSpawners) {
                    LivingEntity entity = (LivingEntity) world.spawnEntity(location, EntityType.STRAY);
                    entity.setRemoveWhenFarAway(false);
                }
            }
        }.runTaskLater(TGTTOS.getInstance(), 0L);
    }

    private void spawnChicken() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, User> userEntry : users.entrySet()) {
                    if (userEntry.getValue().getPlayerStatus() == PlayerStatusEnum.PLAYER) {
                        LivingEntity entity = (LivingEntity) world.spawnEntity(areaChickenSpawnPoint, EntityType.CHICKEN);
                        entity.setRemoveWhenFarAway(false);
//                        entity = (LivingEntity) world.spawnEntity(areaChickenSpawnPoint, EntityType.CHICKEN);
//                        entity.setRemoveWhenFarAway(false);
                    }
                }
            }
        }.runTaskLater(TGTTOS.getInstance(), 0L);
    }

    private void giveBoatToAllPlayers() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, User> userEntry : users.entrySet()) {
                    giveBoatToUser(userEntry.getValue());
                }
            }
        }.runTaskLater(TGTTOS.getInstance(), 0L);
    }

    private void giveRoadToolsToAllPlayers() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, User> userEntry : users.entrySet()) {
                    if (userEntry.getValue().getPlayerStatus() == PlayerStatusEnum.PLAYER) {
                        ItemStack itemStack = new ItemStack(Material.DIAMOND_PICKAXE);
                        ItemMeta itemMeta = itemStack.getItemMeta();
                        if (itemMeta != null)
                            itemMeta.setUnbreakable(true);
                        itemStack.setItemMeta(itemMeta);
                        userEntry.getValue().getPlayer().getInventory().addItem(itemStack);

                        Team team = Team.getTeamByPlayer(userEntry.getValue().getPlayer().getName());
                        if (team != null) {
                            Material material = Material.getMaterial(team.getColorName().toUpperCase() + "_CONCRETE");
                            itemStack = new ItemStack(Objects.requireNonNullElse(material, Material.COBBLESTONE));
                        } else {
                            itemStack = new ItemStack(Material.COBBLESTONE);
                        }
                        itemStack.setAmount(64);
                        itemMeta = itemStack.getItemMeta();
                        if (itemMeta != null)
                            itemMeta.setUnbreakable(true);
                        itemStack.setItemMeta(itemMeta);
                        userEntry.getValue().getPlayer().getInventory().addItem(itemStack);
                    }
                }
            }
        }.runTaskLater(TGTTOS.getInstance(), 0L);
    }

    private void teleportAllPlayerToSpawnPoints() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Location> iterator = areaSpawnPoints.iterator();
                for (Map.Entry<UUID, User> userEntry : users.entrySet()) {
                    if (userEntry.getValue().getPlayerStatus() == PlayerStatusEnum.PLAYER) {
                        if (iterator.hasNext())
                            userEntry.getValue().teleport(iterator.next());
                        else {
                            iterator = areaSpawnPoints.iterator();
                            userEntry.getValue().teleport(iterator.next());
                        }
                    }
                }
            }
        }.runTaskLater(TGTTOS.getInstance(), 0L);
    }
}
