package de.craftplay.quests.library;

public record LibraryDefinition(
    String key,
    String groupId,
    String artifactId,
    String version,
    String driverClass
) {

    public String fileName() {
        return artifactId + "-" + version + ".jar";
    }

    public String repositoryPath() {
        return groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + fileName();
    }
}
