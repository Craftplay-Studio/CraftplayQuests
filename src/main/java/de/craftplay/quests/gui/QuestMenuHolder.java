package de.craftplay.quests.gui;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class QuestMenuHolder implements InventoryHolder {

    private final String menuId;
    private final Map<Integer, GuiButton> buttons = new ConcurrentHashMap<>();

    public QuestMenuHolder(String menuId) {
        this.menuId = menuId;
    }

    public String menuId() {
        return menuId;
    }

    public void putButton(GuiButton button) {
        buttons.put(button.slot(), button);
    }

    public GuiButton button(int slot) {
        return buttons.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
