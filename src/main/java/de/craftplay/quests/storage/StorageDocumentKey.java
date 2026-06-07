package de.craftplay.quests.storage;

import java.util.regex.Pattern;

public record StorageDocumentKey(String namespace, String key) {

    private static final Pattern SAFE_PART = Pattern.compile("[A-Za-z0-9_.-]+");

    public StorageDocumentKey {
        if (!SAFE_PART.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid storage namespace: " + namespace);
        }
        if (!SAFE_PART.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid storage key: " + key);
        }
    }

    public String fileName() {
        return key.endsWith(".yml") ? key : key + ".yml";
    }
}
