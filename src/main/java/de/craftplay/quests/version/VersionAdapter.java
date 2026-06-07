package de.craftplay.quests.version;

public interface VersionAdapter {

    VersionFamily family();

    String serverVersion();

    default boolean supportsTextDisplay() {
        return true;
    }
}
