package de.craftplay.quests.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.craftplay.quests.quest.model.ObjectiveType;
import de.craftplay.quests.quest.model.Quest;
import de.craftplay.quests.quest.model.QuestDifficulty;
import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.quest.model.QuestMetadata;
import de.craftplay.quests.quest.model.QuestObjective;
import de.craftplay.quests.quest.model.QuestRequirement;
import de.craftplay.quests.quest.model.QuestReward;
import de.craftplay.quests.quest.model.QuestType;
import de.craftplay.quests.quest.model.RequirementType;
import de.craftplay.quests.quest.model.RewardType;
import de.craftplay.quests.quest.objective.ObjectiveProgressResult;
import de.craftplay.quests.quest.objective.ObjectiveService;
import de.craftplay.quests.quest.player.PlayerQuestData;
import de.craftplay.quests.quest.player.PlayerQuestProgress;
import de.craftplay.quests.quest.requirement.RequirementService;
import de.craftplay.quests.quest.storage.PlayerQuestDataSerializer;
import de.craftplay.quests.quest.storage.QuestSerializer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class QuestDomainTest {

    @Test
    void questSerializerRoundTripsQuestDocuments() {
        QuestId questId = QuestId.of("miner_story_01");
        Quest quest = quest(
            questId,
            List.of(new QuestObjective("break_stone", ObjectiveType.BREAK_BLOCK, "STONE", 16, Map.of())),
            List.of(new QuestReward("points", RewardType.QUEST_POINTS, Map.of("amount", "5"))),
            List.of(new QuestRequirement("intro", RequirementType.QUEST_COMPLETED, "intro_quest", "", Map.of()))
        );

        Quest copy = new QuestSerializer().deserialize(new QuestSerializer().serialize(quest));

        assertEquals(questId, copy.id());
        assertEquals("Minenstart", copy.name());
        assertEquals(QuestType.STORY, copy.metadata().type());
        assertEquals(QuestDifficulty.EASY, copy.metadata().difficulty());
        assertEquals(ObjectiveType.BREAK_BLOCK, copy.objectives().getFirst().type());
        assertEquals(RewardType.QUEST_POINTS, copy.rewards().getFirst().type());
        assertEquals(RequirementType.QUEST_COMPLETED, copy.requirements().getFirst().type());
    }

    @Test
    void objectiveServiceCapsProgressAndReportsCompletion() {
        QuestId questId = QuestId.of("miner_story_01");
        Quest quest = quest(
            questId,
            List.of(new QuestObjective("break_stone", ObjectiveType.BREAK_BLOCK, "STONE", 3, Map.of())),
            List.of(),
            List.of()
        );
        PlayerQuestData data = PlayerQuestData.empty(UUID.randomUUID()).withActiveQuest(questId);
        ObjectiveService service = new ObjectiveService();

        ObjectiveProgressResult first = service.applyProgress(data, List.of(quest), ObjectiveType.BREAK_BLOCK, "stone", 2);
        assertTrue(first.changed());
        assertTrue(first.completedQuests().isEmpty());
        assertEquals(2, first.playerData().progress(questId).orElseThrow().objectiveValue("break_stone"));

        ObjectiveProgressResult second = service.applyProgress(first.playerData(), List.of(quest), ObjectiveType.BREAK_BLOCK, "STONE", 5);
        assertEquals(List.of(questId), second.completedQuests());
        assertEquals(3, second.playerData().progress(questId).orElseThrow().objectiveValue("break_stone"));
    }

    @Test
    void requirementServiceChecksCompletedQuestRequirements() {
        QuestId requiredQuest = QuestId.of("intro_quest");
        Quest quest = quest(
            QuestId.of("follow_up"),
            List.of(),
            List.of(),
            List.of(new QuestRequirement("requires_intro", RequirementType.QUEST_COMPLETED, requiredQuest.value(), "", Map.of()))
        );
        RequirementService service = new RequirementService();
        PlayerQuestData empty = PlayerQuestData.empty(UUID.randomUUID());

        assertFalse(service.checkAccept(quest, empty).allowed());
        assertTrue(service.checkAccept(quest, empty.withCompletedQuest(requiredQuest)).allowed());
    }

    @Test
    void requirementServiceChecksRuntimePlayerFields() {
        Quest quest = quest(
            QuestId.of("prestige_quest"),
            List.of(),
            List.of(),
            List.of(
                new QuestRequirement("rep", RequirementType.REPUTATION, "", "10", Map.of()),
                new QuestRequirement("title", RequirementType.TITLE, "Steinbrecher", "", Map.of()),
                new QuestRequirement("achievement", RequirementType.ACHIEVEMENT, "quest_first", "", Map.of())
            )
        );
        PlayerQuestData data = PlayerQuestData.empty(UUID.randomUUID())
            .addReputation(10)
            .unlockTitle("Steinbrecher")
            .unlockAchievement("quest_first");

        assertTrue(new RequirementService().checkAccept(quest, data).allowed());
    }

    @Test
    void playerQuestDataSerializerKeepsRuntimeFields() {
        UUID playerId = UUID.randomUUID();
        QuestId questId = QuestId.of("miner_story_01");
        PlayerQuestProgress progress = PlayerQuestProgress.started(questId).withObjectiveProgress("break_stone", 4);
        PlayerQuestData data = PlayerQuestData.empty(playerId)
            .withActiveQuest(questId)
            .withProgress(progress)
            .withTrackedQuest(Optional.of(questId))
            .addQuestPoints(10)
            .addReputation(3)
            .unlockTitle("Steinbrecher")
            .unlockAchievement("quest_first");

        PlayerQuestData copy = new PlayerQuestDataSerializer().deserialize(
            UUID.randomUUID(),
            new PlayerQuestDataSerializer().serialize(data)
        );

        assertEquals(playerId, copy.playerId());
        assertTrue(copy.isActive(questId));
        assertEquals(Optional.of(questId), copy.trackedQuest());
        assertEquals(10, copy.questPoints());
        assertEquals(3, copy.reputation());
        assertTrue(copy.unlockedTitles().contains("Steinbrecher"));
        assertTrue(copy.achievements().contains("quest_first"));
        assertEquals(4, copy.progress(questId).orElseThrow().objectiveValue("break_stone"));
    }

    private Quest quest(
        QuestId questId,
        List<QuestObjective> objectives,
        List<QuestReward> rewards,
        List<QuestRequirement> requirements
    ) {
        return new Quest(
            questId,
            "Minenstart",
            List.of("Baue Stein ab."),
            new QuestMetadata("mining", QuestType.STORY, QuestDifficulty.EASY, 1, 1, "5m", Optional.of("miner"), true),
            Set.of(),
            Set.of(),
            requirements,
            objectives,
            rewards
        );
    }
}
