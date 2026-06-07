package de.craftplay.quests.quest.seed;

import de.craftplay.quests.quest.model.ObjectiveType;
import de.craftplay.quests.quest.model.Quest;
import de.craftplay.quests.quest.model.QuestDifficulty;
import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.quest.model.QuestMetadata;
import de.craftplay.quests.quest.model.QuestObjective;
import de.craftplay.quests.quest.model.QuestReward;
import de.craftplay.quests.quest.model.QuestType;
import de.craftplay.quests.quest.model.RewardType;
import de.craftplay.quests.quest.registry.QuestRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class QuestSeedService {

    public CompletableFuture<Void> seedDefaults(QuestRegistry registry) {
        if (registry.size() > 0) {
            return CompletableFuture.completedFuture(null);
        }

        Quest quest = new Quest(
            QuestId.of("miner_story_01"),
            "Der erste Erzfund",
            List.of("Baue 250 Stein ab und starte deine Bergarbeitergeschichte."),
            new QuestMetadata("mining", QuestType.STORY, QuestDifficulty.NORMAL, 1, 1, "15m", Optional.empty(), true),
            Set.of(),
            Set.of(QuestId.of("miner_story_02")),
            List.of(),
            List.of(new QuestObjective("break_stone", ObjectiveType.BREAK_BLOCK, "STONE", 250, Map.of())),
            List.of(new QuestReward("money", RewardType.MONEY, Map.of("amount", "500", "currency", "taler")))
        );

        return registry.save(quest);
    }
}
