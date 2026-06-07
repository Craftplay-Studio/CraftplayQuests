package de.craftplay.quests.version;

public record SimpleVersionAdapter(VersionFamily family, String serverVersion) implements VersionAdapter {
}
