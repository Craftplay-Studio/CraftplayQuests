package de.craftplay.quests.quest.reward;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.model.Quest;
import de.craftplay.quests.quest.model.QuestReward;
import de.craftplay.quests.quest.model.RewardType;
import de.craftplay.quests.quest.player.PlayerQuestData;
import de.craftplay.quests.scheduler.MainThreadService;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class RewardService {

    private final CraftplayQuestsPlugin plugin;
    private final MainThreadService mainThreadService;

    public RewardService(CraftplayQuestsPlugin plugin, MainThreadService mainThreadService) {
        this.plugin = plugin;
        this.mainThreadService = mainThreadService;
    }

    public RewardPlan planRewards(Quest quest) {
        ArrayList<String> warnings = new ArrayList<>();
        for (QuestReward reward : quest.rewards()) {
            if (reward.data().isEmpty()) {
                warnings.add("reward-has-no-data:" + reward.id());
            }
        }
        return new RewardPlan(quest.id(), quest.rewards(), warnings);
    }

    public PlayerQuestData applyRewards(PlayerQuestData data, Quest quest) {
        PlayerQuestData updated = data;
        for (QuestReward reward : quest.rewards()) {
            updated = applyReward(updated, reward);
        }
        return updated;
    }

    private PlayerQuestData applyReward(PlayerQuestData data, QuestReward reward) {
        if (reward.type() == RewardType.QUEST_POINTS) {
            return data.addQuestPoints(integer(reward, "amount", 0));
        }
        if (reward.type() == RewardType.REPUTATION) {
            return data.addReputation(integer(reward, "amount", 0));
        }
        if (reward.type() == RewardType.TITLE) {
            return data.unlockTitle(reward.data().getOrDefault("title", reward.data().getOrDefault("id", reward.id())));
        }
        if (reward.type() == RewardType.ACHIEVEMENT) {
            return data.unlockAchievement(reward.data().getOrDefault("achievement", reward.data().getOrDefault("id", reward.id())));
        }
        if (reward.type() == RewardType.COMMAND) {
            dispatchCommandReward(data, reward);
        }
        if (reward.type() == RewardType.ITEM) {
            giveItemReward(data, reward);
        }
        if (reward.type() == RewardType.MONEY) {
            depositMoneyReward(data, reward);
        }
        if (reward.type() == RewardType.PERMISSION) {
            grantPermissionReward(data, reward);
        }
        return data;
    }

    private void dispatchCommandReward(PlayerQuestData data, QuestReward reward) {
        String command = reward.data().get("command");
        if (command == null || command.isBlank()) {
            return;
        }
        mainThreadService.execute(() -> {
            Player player = Bukkit.getPlayer(data.playerId());
            String parsedCommand = command
                .replace("%player%", player == null ? data.playerId().toString() : player.getName())
                .replace("{player}", player == null ? data.playerId().toString() : player.getName())
                .replace("{uuid}", data.playerId().toString());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
        });
    }

    private void giveItemReward(PlayerQuestData data, QuestReward reward) {
        String materialName = reward.data().getOrDefault("material", reward.data().getOrDefault("item", ""));
        int amount = Math.max(1, integer(reward, "amount", 1));
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            plugin.getLogger().warning("Invalid item reward material for " + reward.id() + ": " + materialName);
            return;
        }

        mainThreadService.execute(() -> {
            Player player = Bukkit.getPlayer(data.playerId());
            if (player == null) {
                return;
            }
            player.getInventory().addItem(new ItemStack(material, amount));
        });
    }

    private void depositMoneyReward(PlayerQuestData data, QuestReward reward) {
        double amount = decimal(reward, "amount", 0.0D);
        if (amount <= 0.0D || !plugin.services().hooks().enabled("Vault")) {
            return;
        }

        mainThreadService.execute(() -> {
            try {
                Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
                Object registration = Bukkit.getServicesManager().getRegistration(economyClass);
                if (registration == null) {
                    return;
                }
                Object provider = registration.getClass().getMethod("getProvider").invoke(registration);
                OfflinePlayer player = Bukkit.getOfflinePlayer(data.playerId());
                economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class).invoke(provider, player, amount);
            } catch (ReflectiveOperationException exception) {
                plugin.getLogger().warning("Vault money reward failed: " + exception.getMessage());
            }
        });
    }

    private void grantPermissionReward(PlayerQuestData data, QuestReward reward) {
        String permission = reward.data().getOrDefault("permission", reward.data().getOrDefault("node", ""));
        if (permission.isBlank() || !plugin.services().hooks().enabled("LuckPerms")) {
            return;
        }

        mainThreadService.execute(() -> {
            Player player = Bukkit.getPlayer(data.playerId());
            String target = player == null ? data.playerId().toString() : player.getName();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + target + " permission set " + permission + " true");
        });
    }

    private int integer(QuestReward reward, String key, int fallback) {
        try {
            return Integer.parseInt(reward.data().getOrDefault(key, String.valueOf(fallback)));
        } catch (NumberFormatException exception) {
            plugin.getLogger().warning("Invalid reward number for " + reward.id() + ": " + reward.data().get(key));
            return fallback;
        }
    }

    private double decimal(QuestReward reward, String key, double fallback) {
        try {
            return Double.parseDouble(reward.data().getOrDefault(key, String.valueOf(fallback)));
        } catch (NumberFormatException exception) {
            plugin.getLogger().warning("Invalid reward decimal for " + reward.id() + ": " + reward.data().get(key));
            return fallback;
        }
    }
}
