package de.craftplay.quests.quest.reward;

import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.quest.model.QuestReward;
import java.util.List;

public record RewardPlan(
    QuestId questId,
    List<QuestReward> rewards,
    List<String> warnings
) {

    public RewardPlan {
        rewards = rewards == null ? List.of() : List.copyOf(rewards);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
