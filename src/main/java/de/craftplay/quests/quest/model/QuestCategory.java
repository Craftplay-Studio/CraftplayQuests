package de.craftplay.quests.quest.model;

import java.util.Locale;
import java.util.regex.Pattern;

public record QuestCategory(String id, String displayName, boolean enabled) {

    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9][a-z0-9_.-]{1,63}");

    public QuestCategory {
        id = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        displayName = displayName == null || displayName.isBlank() ? id : displayName.trim();
        if (!VALID_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid quest category id: " + id);
        }
    }
}
