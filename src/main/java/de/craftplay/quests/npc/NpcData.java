package de.craftplay.quests.npc;

import de.craftplay.quests.quest.model.QuestId;
import java.util.List;
import java.util.OptionalInt;

public record NpcData(
    String id,
    OptionalInt citizensId,
    String displayName,
    String skin,
    String location,
    List<QuestId> quests,
    List<String> routePoints
) {

    public NpcData {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("NPC id must not be blank.");
        }
        id = id.trim().toLowerCase(java.util.Locale.ROOT);
        citizensId = citizensId == null ? OptionalInt.empty() : citizensId;
        displayName = displayName == null || displayName.isBlank() ? id : displayName.trim();
        skin = skin == null ? "" : skin.trim();
        location = location == null ? "" : location.trim();
        quests = quests == null ? List.of() : List.copyOf(quests);
        routePoints = routePoints == null ? List.of() : List.copyOf(routePoints);
    }
}
