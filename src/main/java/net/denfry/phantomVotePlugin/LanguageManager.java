package net.denfry.phantomVotePlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class LanguageManager {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, YamlConfiguration> languageFiles = new HashMap<>();
    private final String defaultLanguage;
    private final Map<UUID, String> playerLanguages = new HashMap<>();
    private boolean initialized = false;
    private static final String FALLBACK_LANGUAGE = "en";

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.defaultLanguage = plugin.getConfig().getString("language", FALLBACK_LANGUAGE);
        loadLanguages();
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void loadLanguages() {
        File langFolder = new File(plugin.getDataFolder(), "languages");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        File defaultLangFile = new File(langFolder, "messages_en.yml");
        if (!defaultLangFile.exists()) {
            try {
                defaultLangFile.createNewFile();
                YamlConfiguration defaultConfig = new YamlConfiguration();
                defaultConfig.set("vote_start", "<green>Phantom vote started! Use <yellow>/vote yes</yellow> or <yellow>/vote no</yellow>.");
                defaultConfig.set("vote_start_actionbar", "<yellow>Vote started! /vote yes|no");
                defaultConfig.set("vote_end_success", "<green><bold>Vote successful! Phantoms will not spawn this night.");
                defaultConfig.set("vote_end_success_actionbar", "<green>Phantoms disabled for this night!");
                defaultConfig.set("vote_end_fail", "<red>Vote failed. Phantoms will spawn as usual.");
                defaultConfig.set("vote_end_fail_actionbar", "<red>Vote failed.");
                defaultConfig.set("vote_cast", "<green>You voted for <bold>%s</bold>.");
                defaultConfig.set("not_eligible", "<red>You are not eligible to vote.");
                defaultConfig.set("voting_not_active", "<red>Voting is not active.");
                defaultConfig.set("status_not_active", "<yellow>Voting is not active.");
                defaultConfig.set("status_active", "<yellow>Voting active. Yes votes: <green>%yes%</green>, No votes: <red>%no%</red>. Required: <bold>%required%</bold> of %total%.");
                defaultConfig.set("no_permission", "<red>You don't have permission to use this command.");
                defaultConfig.set("player_only", "<red>This command is for players only.");
                defaultConfig.set("invalid_usage", "<red>Usage: /phantomvote status|reloadlang|reload|enable");
                defaultConfig.set("language_set", "<green>Language set to <yellow>%lang%</yellow>.");
                defaultConfig.set("language_invalid", "<red>Language <yellow>%lang%</yellow> not found.");
                defaultConfig.set("language_reloaded", "<green>Language files reloaded successfully.");
                defaultConfig.set("vote_usage", "<red>Usage: /vote yes|no");
                defaultConfig.set("plugin_reloaded", "<green>Plugin reloaded successfully.");
                defaultConfig.set("plugin_enabled", "<green>Plugin enabled successfully.");
                defaultConfig.set("plugin_disabled", "<red>Plugin is disabled. Use /phantomvote enable to enable.");
                defaultConfig.set("vote_start_broadcast", "<green>Voting started with %count% %participant%.");
                defaultConfig.save(defaultLangFile);
                plugin.getLogger().info("Создан файл языка по умолчанию: messages_en.yml");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Не удалось создать файл языка по умолчанию (messages_en.yml): ", e);
                return;
            }
        }

        File ruLangFile = new File(langFolder, "messages_ru.yml");
        if (!ruLangFile.exists()) {
            plugin.saveResource("languages/messages_ru.yml", false);
        }

        languageFiles.clear();
        File[] files = langFolder.listFiles((dir, name) -> name.startsWith("messages_") && name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String langCode = file.getName().replace("messages_", "").replace(".yml", "");
                languageFiles.put(langCode, YamlConfiguration.loadConfiguration(file));
                plugin.getLogger().info("Загружен языковой файл: " + langCode);
            }
        }

        if (languageFiles.containsKey(FALLBACK_LANGUAGE)) {
            initialized = true;
        } else {
            plugin.getLogger().warning("Файл языка по умолчанию (" + FALLBACK_LANGUAGE + ") не загружен!");
        }
    }

    public Component getMessage(Player player, String key, String... placeholders) {
        String lang = player != null ? playerLanguages.getOrDefault(player.getUniqueId(), defaultLanguage) : defaultLanguage;
        return getMessage(lang, key, placeholders);
    }

    public Component getMessage(String lang, String key, String... placeholders) {
        if (lang == null) {
            lang = defaultLanguage;
        }
        String message = getRawMessage(lang, key);
        if (message == null) {
            message = getRawMessage(FALLBACK_LANGUAGE, key);
            if (message == null) {
                plugin.getLogger().warning("Сообщение не найдено ни в одном языке: " + key);
                return Component.text("Message not found: " + key);
            }
            plugin.getLogger().warning("Сообщение '" + key + "' не найдено в языке '" + lang + "', используется fallback: " + FALLBACK_LANGUAGE);
        }

        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }

        return miniMessage.deserialize(message);
    }

    private String getRawMessage(String lang, String key) {
        YamlConfiguration config = languageFiles.get(lang);
        return config != null ? config.getString(key) : null;
    }

    public void setPlayerLanguage(Player player, String language) {
        if (languageFiles.containsKey(language)) {
            playerLanguages.put(player.getUniqueId(), language);
            player.sendMessage(getMessage(player, "language_set", "%lang%", language));
        } else {
            player.sendMessage(getMessage(player, "language_invalid", "%lang%", language));
        }
    }

    public void reloadLanguages() {
        loadLanguages();
        plugin.getLogger().info("Языковые файлы перезагружены.");
    }

    public void savePlayerLanguages() {
        File file = new File(plugin.getDataFolder(), "player_languages.yml");
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, String> entry : playerLanguages.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при сохранении языковых настроек: ", e);
        }
    }

    public void loadPlayerLanguages() {
        File file = new File(plugin.getDataFolder(), "player_languages.yml");
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String lang = config.getString(key);
                if (languageFiles.containsKey(lang)) {
                    playerLanguages.put(uuid, lang);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Некорректный UUID в player_languages.yml: " + key);
            }
        }
    }
}