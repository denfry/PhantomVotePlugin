package net.denfry.phantomVotePlugin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.logging.Level;

public class PhantomVotePlugin extends JavaPlugin implements Listener {
    private VoteManager voteManager;
    private LanguageManager languageManager;
    private long lastTime;
    private BukkitTask voteTask;
    private boolean preventPhantomSpawn = false;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        getLogger().info("PhantomVote: Подготовка к загрузке плагина...");
    }

    @Override
    public void onEnable() {
        try {
            initializePlugin();
            getServer().getPluginManager().registerEvents(this, this);
            getLogger().info("PhantomVote успешно активирован!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Ошибка при активации плагина: ", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        cleanup();
        getLogger().info("PhantomVote деактивирован!");
    }

    private void initializePlugin() {
        // Инициализация мультиязычности
        languageManager = new LanguageManager(this);
        languageManager.loadPlayerLanguages();

        // Проверка успешной загрузки языков
        if (!languageManager.isInitialized()) {
            throw new IllegalStateException("Не удалось инициализировать LanguageManager.");
        }

        // Инициализация голосования
        double threshold = getConfig().getDouble("voting.threshold", 0.5);
        List<String> worlds = getConfig().getStringList("worlds");
        voteManager = new VoteManager(threshold, languageManager, this);
        voteManager.loadVotes();

        // Регистрация команд
        if (getCommand("vote") == null) {
            getLogger().severe("Команда 'vote' не найдена в plugin.yml!");
        } else {
            getCommand("vote").setExecutor(new VoteCommand(voteManager, languageManager, this));
        }
        if (getCommand("phantomvote") == null) {
            getLogger().severe("Команда 'phantomvote' не найдена в plugin.yml!");
        } else {
            getCommand("phantomvote").setExecutor(new VoteCommand(voteManager, languageManager, this));
        }

        // Логирование выбранного мира один раз при инициализации
        String selectedWorld = getWorldName(worlds);
        if (selectedWorld != null) {
            getLogger().info("Для голосования будет использоваться мир: " + selectedWorld);
        } else {
            getLogger().warning("Не удалось выбрать мир для голосования. Голосование не будет запущено.");
        }

        // Отложенная инициализация расписания голосования
        new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getWorlds().isEmpty()) {
                    getLogger().warning("Миров на сервере нет! Ожидание загрузки миров...");
                    return;
                }
                lastTime = getWorldTime(worlds);
                voteTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        long currentTime = getWorldTime(worlds);
                        if (currentTime >= 13000 && lastTime < 13000) {
                            preventPhantomSpawn = false; // Сбрасываем флаг в начале ночи
                            voteManager.startVote();
                        }
                        if (currentTime < 13000 && lastTime >= 13000) {
                            voteManager.endVote();
                        }
                        lastTime = currentTime;
                    }
                }.runTaskTimer(PhantomVotePlugin.this, 0L, 20L);
                getLogger().info("Расписание голосования успешно запущено.");
            }
        }.runTaskLater(this, 20L); // Ожидание 1 секунды для загрузки миров
    }

    private void cleanup() {
        if (voteManager != null) {
            voteManager.saveVotes();
            voteManager.endVote();
        }
        if (languageManager != null) {
            languageManager.savePlayerLanguages();
        }
        if (voteTask != null) {
            voteTask.cancel();
            voteTask = null;
        }
    }

    private long getWorldTime(List<String> worlds) {
        for (String worldName : worlds) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                return world.getTime();
            }
        }

        World defaultWorld = Bukkit.getWorld("world");
        if (defaultWorld != null) {
            return defaultWorld.getTime();
        }

        List<World> availableWorlds = Bukkit.getWorlds();
        if (!availableWorlds.isEmpty()) {
            return availableWorlds.get(0).getTime();
        }

        getLogger().warning("Не удалось найти ни один мир для отслеживания времени!");
        return 0L;
    }

    private String getWorldName(List<String> worlds) {
        for (String worldName : worlds) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                return worldName;
            }
        }

        World defaultWorld = Bukkit.getWorld("world");
        if (defaultWorld != null) {
            return "world";
        }

        List<World> availableWorlds = Bukkit.getWorlds();
        if (!availableWorlds.isEmpty()) {
            return availableWorlds.get(0).getName();
        }

        return null;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public VoteManager getVoteManager() {
        return voteManager;
    }

    public void reloadPlugin() {
        cleanup();
        initializePlugin();
        getLogger().info("PhantomVote успешно перезагружен!");
    }

    public void enablePlugin() {
        if (getServer().getPluginManager().isPluginEnabled(this)) {
            getLogger().info("PhantomVote уже активирован.");
            return;
        }
        initializePlugin();
        getServer().getPluginManager().enablePlugin(this);
        getLogger().info("PhantomVote повторно активирован!");
    }

    public void setPreventPhantomSpawn(boolean prevent) {
        this.preventPhantomSpawn = prevent;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() == EntityType.PHANTOM && preventPhantomSpawn) {
            List<String> worlds = getConfig().getStringList("worlds");
            if (worlds.isEmpty() || worlds.contains(event.getLocation().getWorld().getName())) {
                event.setCancelled(true);
            }
        }
    }
}