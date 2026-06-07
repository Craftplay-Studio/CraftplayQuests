package de.craftplay.quests.version;

import org.bukkit.Bukkit;

public final class MinecraftVersionDetector {

    public VersionAdapter detect() {
        String version = Bukkit.getMinecraftVersion();
        if (version.startsWith("1.21")) {
            return new SimpleVersionAdapter(VersionFamily.V1_21, version);
        }
        if (version.startsWith("26.1")) {
            return new SimpleVersionAdapter(VersionFamily.V26_1, version);
        }
        return new SimpleVersionAdapter(VersionFamily.FALLBACK, version);
    }
}
