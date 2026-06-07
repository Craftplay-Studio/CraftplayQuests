package de.craftplay.quests.quest.requirement;

import java.util.List;

public record RequirementCheckResult(
    boolean allowed,
    List<String> missingReasons
) {

    public RequirementCheckResult {
        missingReasons = missingReasons == null ? List.of() : List.copyOf(missingReasons);
    }

    public static RequirementCheckResult success() {
        return new RequirementCheckResult(true, List.of());
    }

    public static RequirementCheckResult denied(List<String> reasons) {
        return new RequirementCheckResult(false, reasons);
    }
}
