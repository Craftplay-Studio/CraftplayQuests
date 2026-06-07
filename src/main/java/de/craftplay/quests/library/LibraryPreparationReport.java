package de.craftplay.quests.library;

import java.nio.file.Path;
import java.util.List;

public record LibraryPreparationReport(
    List<Path> cached,
    List<Path> downloaded,
    List<LibraryDefinition> missing,
    List<LibraryDefinition> skipped
) {
}
