package de.craftplay.quests.quest.model;

public enum QuestDifficulty {
    VERY_EASY("WHEAT_SEEDS", "★☆☆☆☆"),
    EASY("WOODEN_SWORD", "★★☆☆☆"),
    NORMAL("IRON_SWORD", "★★★☆☆"),
    HARD("DIAMOND_SWORD", "★★★★☆"),
    EXPERT("NETHERITE_SWORD", "★★★★★"),
    LEGENDARY("NETHER_STAR", "★★★★★");

    private final String iconMaterial;
    private final String stars;

    QuestDifficulty(String iconMaterial, String stars) {
        this.iconMaterial = iconMaterial;
        this.stars = stars;
    }

    public String iconMaterial() {
        return iconMaterial;
    }

    public String stars() {
        return stars;
    }
}
