package de.craftplay.quests.quest.objective;

import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.quest.player.PlayerQuestData;
import java.util.List;

public record ObjectiveProgressResult(
    PlayerQuestData playerData,
    List<QuestId> changedQuests,
    List<QuestId> completedQuests
) {

    public ObjectiveProgressResult {
        changedQuests = changedQuests == null ? List.of() : List.copyOf(changedQuests);
        completedQuests = completedQuests == null ? List.of() : List.copyOf(completedQuests);
    }

    public boolean changed() {
        return !changedQuests.isEmpty();
    }
}
