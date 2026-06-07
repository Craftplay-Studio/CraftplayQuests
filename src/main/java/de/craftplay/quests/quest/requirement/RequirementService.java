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

        if (requirement.type() == RequirementType.REPUTATION) {
            int requiredReputation = integer(requirement.value().isBlank() ? requirement.target() : requirement.value(), 0);
            if (data.reputation() < requiredReputation) {
                return Optional.of("reputation:" + requiredReputation);
            }
            return Optional.empty();
        }

        if (requirement.type() == RequirementType.TITLE) {
            String title = requirement.target().isBlank() ? requirement.value() : requirement.target();
            if (title.isBlank()) {
                return Optional.of("requirement-invalid:" + requirement.id());
            }
            if (!data.unlockedTitles().contains(title)) {
                return Optional.of("title:" + title);
            }
            return Optional.empty();
        }

        if (requirement.type() == RequirementType.ACHIEVEMENT) {
            String achievement = requirement.target().isBlank() ? requirement.value() : requirement.target();
            if (achievement.isBlank()) {
                return Optional.of("requirement-invalid:" + requirement.id());
            }
            if (!data.achievements().contains(achievement)) {
                return Optional.of("achievement:" + achievement);
            }
            return Optional.empty();
        }

        return Optional.empty();
    }

    private int integer(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
