package de.craftplay.quests.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class QuestMenuHolder implements InventoryHolder {

    private final String menuId;

    public QuestMenuHolder(String menuId) {
        this.menuId = menuId;
    }

    public String menuId() {
        return menuId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
