package net.denfry.phantomVotePlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VoteCommand implements CommandExecutor {
    private final VoteManager voteManager;
    private final LanguageManager languageManager;
    private final PhantomVotePlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 5000; // 5 секунд в миллисекундах

    public VoteCommand(VoteManager voteManager, LanguageManager languageManager, PhantomVotePlugin plugin) {
        this.voteManager = voteManager;
        this.languageManager = languageManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(languageManager.getMessage((String) null, "player_only"));
            return true;
        }
        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        // Проверка кулдауна
        long currentTime = System.currentTimeMillis();
        if (cooldowns.containsKey(playerId) && currentTime - cooldowns.get(playerId) < COOLDOWN_TIME) {
            long remainingSeconds = (COOLDOWN_TIME - (currentTime - cooldowns.get(playerId))) / 1000 + 1;
            player.sendMessage(languageManager.getMessage(player, "command_cooldown", "%seconds%", String.valueOf(remainingSeconds)));
            plugin.getLogger().info("Игрок " + player.getName() + " пытался спамить команду /" + label);
            return true;
        }

        if (command.getName().equalsIgnoreCase("phantomvote")) {
            if (!player.hasPermission("phantomvote.admin")) {
                player.sendMessage(languageManager.getMessage(player, "no_permission"));
                return true;
            }
            if (args.length == 0) {
                player.sendMessage(languageManager.getMessage(player, "invalid_usage"));
                return true;
            }
            if (args[0].equalsIgnoreCase("status")) {
                player.sendMessage(voteManager.getStatus(player));
                return true;
            }
            if (args[0].equalsIgnoreCase("reloadlang")) {
                languageManager.reloadLanguages();
                player.sendMessage(languageManager.getMessage(player, "language_reloaded"));
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                plugin.reloadPlugin();
                player.sendMessage(languageManager.getMessage(player, "plugin_reloaded"));
                return true;
            }
            if (args[0].equalsIgnoreCase("enable")) {
                plugin.enablePlugin();
                player.sendMessage(languageManager.getMessage(player, "plugin_enabled"));
                return true;
            }
            player.sendMessage(languageManager.getMessage(player, "invalid_usage"));
            return true;
        }

        if (!plugin.getServer().getPluginManager().isPluginEnabled(plugin)) {
            player.sendMessage(languageManager.getMessage(player, "plugin_disabled"));
            return true;
        }

        if (!voteManager.isVotingActive()) {
            player.sendMessage(languageManager.getMessage(player, "voting_not_active"));
            return true;
        }
        if (!voteManager.isEligibleVoter(playerId)) {
            player.sendMessage(languageManager.getMessage(player, "not_eligible"));
            return true;
        }
        if (args.length != 1 || (!args[0].equalsIgnoreCase("yes") && !args[0].equalsIgnoreCase("no"))) {
            player.sendMessage(languageManager.getMessage(player, "vote_usage"));
            return true;
        }
        if (voteManager.hasVoted(playerId)) {
            player.sendMessage(languageManager.getMessage(player, "already_voted"));
            plugin.getLogger().info("Игрок " + player.getName() + " пытался проголосовать повторно");
            return true;
        }

        // Устанавливаем кулдаун
        cooldowns.put(playerId, currentTime);

        boolean vote = args[0].equalsIgnoreCase("yes");
        voteManager.addVote(playerId, vote);
        player.sendMessage(languageManager.getMessage(player, "vote_cast", "%s", vote ? "да" : "нет"));
        return true;
    }
}