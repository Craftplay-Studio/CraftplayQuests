package de.craftplay.quests.quest.requirement;

import de.craftplay.quests.quest.model.Quest;
import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.quest.model.QuestRequirement;
import de.craftplay.quests.quest.model.RequirementType;
import de.craftplay.quests.quest.player.PlayerQuestData;
import java.util.ArrayList;
import java.util.Optional;

public final class RequirementService {

    public RequirementCheckResult checkAccept(Quest quest, PlayerQuestData data) {
        ArrayList<String> missing = new ArrayList<>();

        if (!quest.enabled()) {
            missing.add("quest-disabled");
        }
        if (data.isActive(quest.id())) {
            missing.add("quest-already-active");
        }
        if (data.isCompleted(quest.id())) {
            missing.add("quest-already-completed");
        }

        for (QuestId requiredQuest : quest.requiresCompleted()) {
            if (!data.completedQuests().contains(requiredQuest)) {
                missing.add("requires-completed:" + requiredQuest.value());
            }
        }

        for (QuestRequirement requirement : quest.requirements()) {
            Optional<String> missingReason = evaluateRequirement(requirement, data);
            missingReason.ifPresent(missing::add);
        }

        if (missing.isEmpty()) {
            return RequirementCheckResult.success();
        }
        return RequirementCheckResult.denied(missing);
    }

    private Optional<String> evaluateRequirement(QuestRequirement requirement, PlayerQuestData data) {
        if (requirement.type() == RequirementType.QUEST_COMPLETED) {
            String questId = requirement.target().isBlank() ? requirement.value() : requirement.target();
            if (questId.isBlank()) {
                return Optional.of("requirement-invalid:" + requirement.id());
            }
            if (!data.completedQuests().contains(QuestId.of(questId))) {
                return Optional.of("quest-completed:" + questId);
            }
            return Optional.empty();
        }

        return Optional.of("requirement-not-implemented:" + requirement.type().name());
    }
}
