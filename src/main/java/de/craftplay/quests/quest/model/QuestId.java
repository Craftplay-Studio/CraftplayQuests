package de.craftplay.quests.quest.model;

import java.util.Locale;
import java.util.regex.Pattern;

public record QuestId(String value) {

    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9][a-z0-9_.-]{1,63}");

    public QuestId {
        value = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!VALID_ID.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid quest id: " + value);
        }
    }

    public static QuestId of(String value) {
        return new QuestId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
