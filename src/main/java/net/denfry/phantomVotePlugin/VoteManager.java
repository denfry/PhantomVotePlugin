package net.denfry.phantomVotePlugin;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class VoteManager {
    private final double threshold;
    private final LanguageManager languageManager;
    private final PhantomVotePlugin plugin;
    private boolean votingActive = false;
    private final Map<UUID, Boolean> votes = new HashMap<>();
    private final Set<UUID> eligibleVoters = new HashSet<>();

    public VoteManager(double threshold, LanguageManager languageManager, PhantomVotePlugin plugin) {
        this.threshold = threshold;
        this.languageManager = languageManager;
        this.plugin = plugin;
    }

    public void startVote() {
        if (votingActive) return;
        votingActive = true;
        votes.clear();
        eligibleVoters.clear();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isEligibleVoter(player.getUniqueId())) {
                eligibleVoters.add(player.getUniqueId());
            }
        }

        String participantWord = getParticipantWord(eligibleVoters.size());
        Bukkit.broadcast(languageManager.getMessage((String) null, "vote_start"));
        Bukkit.broadcast(languageManager.getMessage((String) null, "vote_start_broadcast", "%count%", String.valueOf(eligibleVoters.size()), "%participant%", participantWord));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(languageManager.getMessage(player, "vote_start_actionbar"));
        }

        plugin.getLogger().info("Голосование началось с " + eligibleVoters.size() + " " + participantWord + ".");
    }

    public void endVote() {
        if (!votingActive) return;
        votingActive = false;

        int yesVotes = 0;
        for (Boolean vote : votes.values()) {
            if (vote) yesVotes++;
        }

        int requiredVotes = (int) Math.ceil(eligibleVoters.size() * threshold);
        if (yesVotes >= requiredVotes) {
            plugin.setPreventPhantomSpawn(true);
            Bukkit.broadcast(languageManager.getMessage((String) null, "vote_end_success"));
            Bukkit.broadcast(languageManager.getMessage((String) null, "vote_end_success_broadcast"));
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isEligibleVoter(player.getUniqueId())) {
                    player.sendActionBar(languageManager.getMessage(player, "vote_end_success_actionbar"));
                }
            }
            plugin.getLogger().info("Голосование успешно: фантомы отключены на эту ночь.");
        } else {
            Bukkit.broadcast(languageManager.getMessage((String) null, "vote_end_fail"));
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isEligibleVoter(player.getUniqueId())) {
                    player.sendActionBar(languageManager.getMessage(player, "vote_end_fail_actionbar"));
                }
            }
            plugin.getLogger().info("Голосование провалилось: фантомы будут спавниться.");
        }

        votes.clear();
        eligibleVoters.clear();
    }

    public void addVote(UUID playerId, boolean vote) {
        if (!votingActive || !eligibleVoters.contains(playerId)) return;
        votes.put(playerId, vote);
    }

    public boolean hasVoted(UUID playerId) {
        return votes.containsKey(playerId);
    }

    public boolean isVotingActive() {
        return votingActive;
    }

    public boolean isEligibleVoter(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return false;
        List<String> worlds = plugin.getConfig().getStringList("worlds");
        return worlds.isEmpty() || worlds.contains(player.getWorld().getName());
    }

    public Component getStatus(Player player) {
        if (!votingActive) {
            return languageManager.getMessage(player, "status_not_active");
        }
        int yesVotes = 0;
        for (Boolean vote : votes.values()) {
            if (vote) yesVotes++;
        }
        int noVotes = votes.size() - yesVotes;
        int requiredVotes = (int) Math.ceil(eligibleVoters.size() * threshold);
        return languageManager.getMessage(player, "status_active",
                "%yes%", String.valueOf(yesVotes),
                "%no%", String.valueOf(noVotes),
                "%required%", String.valueOf(requiredVotes),
                "%total%", String.valueOf(eligibleVoters.size()));
    }

    public void loadVotes() {
        File file = new File(plugin.getDataFolder(), "votes.yml");
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                votes.put(uuid, config.getBoolean(key));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Некорректный UUID в votes.yml: " + key);
            }
        }
    }

    public void saveVotes() {
        File file = new File(plugin.getDataFolder(), "votes.yml");
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Boolean> entry : votes.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при сохранении голосов: ", e);
        }
    }

    private String getParticipantWord(int count) {
        if (count == 1) return "участником";
        if (count >= 2 && count <= 4) return "участниками";
        return "участниками";
    }
}