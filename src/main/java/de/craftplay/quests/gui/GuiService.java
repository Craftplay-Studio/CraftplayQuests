package de.craftplay.quests.gui;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.model.Quest;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class GuiService {

    private final CraftplayQuestsPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, GuiButton> openButtons = new HashMap<>();

    public GuiService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {
        YamlConfiguration configuration = loadGui("main.yml");
        int size = configuration.getInt("size", 54);
        Inventory inventory = Bukkit.createInventory(new QuestMenuHolder("main"), size, miniMessage.deserialize(configuration.getString("title", "<gold>Craftplay Quests</gold>")));
        openButtons.clear();

        addConfiguredButtons(inventory, configuration.getConfigurationSection("buttons"), player);
        addStaticButton(inventory, configuration.getConfigurationSection("close-button"), player);
        player.openInventory(inventory);
    }

    public void openAdventureBook(Player player) {
        openConfigured(player, "adventure_book.yml", "adventure_book", "<gold>Abenteuerbuch</gold>");
    }

    public void openAchievements(Player player) {
        openConfigured(player, "achievements.yml", "achievements", "<gold>Achievements</gold>");
    }

    public void openPlayerQuests(Player player) {
        Inventory inventory = Bukkit.createInventory(new QuestMenuHolder("player_quests"), 54, miniMessage.deserialize("<gold>Meine Quests</gold>"));
        int slot = 10;
        for (Quest quest : plugin.services().quests().registry().sortedById()) {
            if (slot >= 44) {
                break;
            }
            ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(miniMessage.deserialize("<yellow>" + quest.name() + "</yellow>"));
            meta.lore(List.of(
                miniMessage.deserialize("<gray>ID: <yellow>" + quest.id().value() + "</yellow></gray>"),
                miniMessage.deserialize("<gray>Typ: <aqua>" + quest.metadata().type().name() + "</aqua></gray>"),
                miniMessage.deserialize("<green>Klicken zum Annehmen.</green>")
            ));
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
            openButtons.put("player_quests:" + slot, new GuiButton(slot, "ACCEPT_QUEST:" + quest.id().value()));
            slot++;
        }
        player.openInventory(inventory);
    }

    public GuiButton button(String menuId, int slot) {
        return openButtons.get(menuId + ":" + slot);
    }

    private void openConfigured(Player player, String fileName, String menuId, String defaultTitle) {
        YamlConfiguration configuration = loadGui(fileName);
        int size = configuration.getInt("size", 54);
        Inventory inventory = Bukkit.createInventory(
            new QuestMenuHolder(menuId),
            size,
            miniMessage.deserialize(configuration.getString("title", defaultTitle))
        );
        openButtons.clear();
        addConfiguredButtons(inventory, configuration.getConfigurationSection("buttons"), player);
        addStaticButton(inventory, configuration.getConfigurationSection("back-button"), player);
        addStaticButton(inventory, configuration.getConfigurationSection("close-button"), player);
        player.openInventory(inventory);
    }

    private void addConfiguredButtons(Inventory inventory, ConfigurationSection section, Player player) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            addStaticButton(inventory, section.getConfigurationSection(key), player);
        }
    }

    private void addStaticButton(Inventory inventory, ConfigurationSection section, Player player) {
        if (section == null || !section.getBoolean("enabled", true)) {
            return;
        }

        String permission = section.getString("permission", "");
        if (!permission.isBlank() && section.getBoolean("hide-without-permission", false) && !player.hasPermission(permission)) {
            return;
        }

        int slot = section.getInt("slot", -1);
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        if (material == null) {
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(miniMessage.deserialize(plugin.services().placeholders().apply(player, section.getString("name", "<gray>Button</gray>"))));
        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) {
            meta.lore(lore.stream()
                .map(line -> plugin.services().placeholders().apply(player, line))
                .map(miniMessage::deserialize)
                .toList());
        }
        item.setItemMeta(meta);
        inventory.setItem(slot, item);

        String action = section.getString("action", "");
        if (!action.isBlank()) {
            openButtons.put(((QuestMenuHolder) inventory.getHolder()).menuId() + ":" + slot, new GuiButton(slot, action));
        }
    }

    private YamlConfiguration loadGui(String fileName) {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "gui/" + fileName));
    }
}
