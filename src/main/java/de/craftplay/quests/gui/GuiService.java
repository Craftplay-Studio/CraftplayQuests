package de.craftplay.quests.gui;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.npc.NpcData;
import de.craftplay.quests.quest.model.Quest;
import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.quest.model.QuestMetadata;
import de.craftplay.quests.quest.player.PlayerQuestData;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.kyori.adventure.text.Component;
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

    public GuiService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {
        openConfigured(player, "main.yml", "main", "<gold>Craftplay Quests</gold>");
    }

    public void openAdventureBook(Player player) {
        openConfigured(player, "adventure_book.yml", "adventure_book", "<gold>Abenteuerbuch</gold>");
    }

    public void openAchievements(Player player) {
        openConfigured(player, "achievements.yml", "achievements", "<gold>Achievements</gold>");
    }

    public void openAdmin(Player player) {
        openConfigured(player, "admin_main.yml", "admin_main", "<dark_red>CraftplayQuests Admin</dark_red>");
    }

    public void openAdminQuests(Player player) {
        YamlConfiguration configuration = loadGui("admin_quests.yml");
        OpenMenu menu = createMenu(configuration, "admin_quests", "<dark_red>Admin: Quests</dark_red>");
        addConfiguredButtons(menu, configuration.getConfigurationSection("buttons"), player);
        fillQuestList(menu, configuration.getIntegerList("quest-slots.slots"));
        addStaticButton(menu, configuration.getConfigurationSection("back-button"), player);
        addStaticButton(menu, configuration.getConfigurationSection("close-button"), player);
        player.openInventory(menu.inventory());
    }

    public void openAdminNpcs(Player player) {
        YamlConfiguration configuration = loadGui("admin_npcs.yml");
        OpenMenu menu = createMenu(configuration, "admin_npcs", "<dark_red>Admin: NPCs</dark_red>");
        addConfiguredButtons(menu, configuration.getConfigurationSection("buttons"), player);
        fillNpcList(menu, configuration.getIntegerList("npc-slots.slots"));
        addStaticButton(menu, configuration.getConfigurationSection("back-button"), player);
        addStaticButton(menu, configuration.getConfigurationSection("close-button"), player);
        player.openInventory(menu.inventory());
    }

    public void openAdminCategories(Player player) {
        openConfigured(player, "admin_categories.yml", "admin_categories", "<dark_red>Admin: Kategorien</dark_red>");
    }

    public void openAdminSettings(Player player) {
        openConfigured(player, "admin_settings.yml", "admin_settings", "<dark_red>Admin: Einstellungen</dark_red>");
    }

    public void openAdminTitles(Player player) {
        plugin.services().quests().allPlayerData()
            .whenComplete((allData, throwable) -> plugin.services().mainThread().execute(() -> {
                if (!player.isOnline()) {
                    return;
                }
                if (throwable != null) {
                    plugin.language().send(player, "errors.generic");
                    return;
                }
                YamlConfiguration configuration = loadGui("admin_titles.yml");
                OpenMenu menu = createMenu(configuration, "admin_titles", "<dark_red>Admin: Titel</dark_red>");
                addConfiguredButtons(menu, configuration.getConfigurationSection("buttons"), player);
                fillTitleList(menu, configuration.getIntegerList("title-slots.slots"), allData);
                addStaticButton(menu, configuration.getConfigurationSection("back-button"), player);
                addStaticButton(menu, configuration.getConfigurationSection("close-button"), player);
                player.openInventory(menu.inventory());
            }));
    }

    public void openQuestDetails(Player player, Quest quest) {
        YamlConfiguration configuration = loadGui("quest_details.yml");
        OpenMenu menu = createMenu(configuration, "quest_details", "<gold>Questdetails</gold>");
        UnaryOperator<String> replacer = value -> questPlaceholders(quest, value);

        ConfigurationSection iconSection = configuration.getConfigurationSection("quest-icon");
        if (iconSection != null) {
            Material material = material(iconSection.getString("material", "WRITABLE_BOOK"));
            addItem(
                menu,
                iconSection.getInt("slot", 13),
                material,
                replacer.apply(iconSection.getString("name", "<yellow>%quest_name%</yellow>")),
                iconSection.getStringList("lore").stream().map(replacer).toList(),
                ""
            );
        }

        addConfiguredButtons(menu, configuration.getConfigurationSection("buttons"), player, replacer, action -> questAction(action, quest));
        addStaticButton(menu, configuration.getConfigurationSection("back-button"), player);
        addStaticButton(menu, configuration.getConfigurationSection("close-button"), player);
        player.openInventory(menu.inventory());
    }

    public void openPlayerQuests(Player player) {
        OpenMenu menu = createMenu(54, "player_quests", "<gold>Meine Quests</gold>");
        int slot = 10;
        for (Quest quest : plugin.services().quests().registry().sortedById()) {
            if (slot >= 44) {
                break;
            }
            addItem(
                menu,
                slot,
                Material.WRITABLE_BOOK,
                "<yellow>" + quest.name() + "</yellow>",
                List.of(
                    "<gray>ID: <yellow>" + quest.id().value() + "</yellow></gray>",
                    "<gray>Typ: <aqua>" + quest.metadata().type().name() + "</aqua></gray>",
                    "<green>Klicken zum Annehmen.</green>"
                ),
                "ACCEPT_QUEST:" + quest.id().value()
            );
            slot++;
        }
        player.openInventory(menu.inventory());
    }

    public void createTemplateQuest(Player player) {
        String id = "admin_quest_" + Long.toString(System.currentTimeMillis(), 36);
        Quest quest = new Quest(
            QuestId.of(id),
            "Neue Quest " + id,
            List.of("Diese Quest wurde im Admin-Menü erstellt."),
            QuestMetadata.defaults(),
            Set.of(),
            Set.of(),
            List.of(),
            List.of(),
            List.of()
        );
        plugin.services().quests().saveQuest(quest)
            .whenComplete((ignored, throwable) -> plugin.services().mainThread().execute(() -> {
                if (throwable != null) {
                    plugin.language().send(player, "errors.generic");
                    return;
                }
                plugin.language().send(player, "admin.saved");
                plugin.services().audit().record(player, "gui-quest-create", id);
                openAdminQuests(player);
            }));
    }

    public void createTemplateNpc(Player player) {
        String id = "admin_npc_" + Long.toString(System.currentTimeMillis(), 36);
        plugin.services().npcs().create(id, "Admin NPC " + id)
            .whenComplete((npc, throwable) -> plugin.services().mainThread().execute(() -> {
                if (throwable != null) {
                    plugin.language().send(player, "errors.generic");
                    return;
                }
                plugin.language().send(player, "npc.created", Map.of("npc", npc.id()));
                plugin.services().audit().record(player, "gui-npc-create", npc.id());
                openAdminNpcs(player);
            }));
    }

    public void createAdminTitle(Player player) {
        String title = "Admin-Titel-" + Long.toString(System.currentTimeMillis(), 36);
        plugin.services().titles().grantTitle(player.getUniqueId(), title)
            .whenComplete((data, throwable) -> plugin.services().mainThread().execute(() -> {
                if (throwable != null) {
                    plugin.language().send(player, "errors.generic");
                    return;
                }
                plugin.language().send(player, "title.unlocked", Map.of("title", title));
                plugin.services().audit().record(player, "gui-title-create", title);
                openAdminTitles(player);
            }));
    }

    private void openConfigured(Player player, String fileName, String menuId, String defaultTitle) {
        YamlConfiguration configuration = loadGui(fileName);
        OpenMenu menu = createMenu(configuration, menuId, defaultTitle);
        addConfiguredButtons(menu, configuration.getConfigurationSection("buttons"), player);
        addStaticButton(menu, configuration.getConfigurationSection("back-button"), player);
        addStaticButton(menu, configuration.getConfigurationSection("close-button"), player);
        player.openInventory(menu.inventory());
    }

    private OpenMenu createMenu(YamlConfiguration configuration, String menuId, String defaultTitle) {
        return createMenu(configuration.getInt("size", 54), menuId, configuration.getString("title", defaultTitle));
    }

    private OpenMenu createMenu(int size, String menuId, String title) {
        QuestMenuHolder holder = new QuestMenuHolder(menuId);
        Inventory inventory = Bukkit.createInventory(holder, size, miniMessage.deserialize(title));
        return new OpenMenu(inventory, holder);
    }

    private void addConfiguredButtons(OpenMenu menu, ConfigurationSection section, Player player) {
        addConfiguredButtons(menu, section, player, UnaryOperator.identity(), UnaryOperator.identity());
    }

    private void addConfiguredButtons(
        OpenMenu menu,
        ConfigurationSection section,
        Player player,
        UnaryOperator<String> textReplacer,
        UnaryOperator<String> actionReplacer
    ) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            addStaticButton(menu, section.getConfigurationSection(key), player, textReplacer, actionReplacer);
        }
    }

    private void addStaticButton(OpenMenu menu, ConfigurationSection section, Player player) {
        addStaticButton(menu, section, player, UnaryOperator.identity(), UnaryOperator.identity());
    }

    private void addStaticButton(
        OpenMenu menu,
        ConfigurationSection section,
        Player player,
        UnaryOperator<String> textReplacer,
        UnaryOperator<String> actionReplacer
    ) {
        if (section == null || !section.getBoolean("enabled", true)) {
            return;
        }

        String permission = section.getString("permission", "");
        if (!permission.isBlank() && section.getBoolean("hide-without-permission", false) && !player.hasPermission(permission)) {
            return;
        }

        String action = actionReplacer.apply(section.getString("action", ""));
        List<String> lore = section.getStringList("lore").stream()
            .map(textReplacer)
            .map(line -> plugin.services().placeholders().apply(player, line))
            .toList();
        addItem(
            menu,
            section.getInt("slot", -1),
            material(section.getString("material", "STONE")),
            plugin.services().placeholders().apply(player, textReplacer.apply(section.getString("name", "<gray>Button</gray>"))),
            lore,
            action
        );
    }

    private void fillQuestList(OpenMenu menu, List<Integer> slots) {
        int index = 0;
        for (Quest quest : plugin.services().quests().registry().sortedById()) {
            if (index >= slots.size()) {
                break;
            }
            addItem(
                menu,
                slots.get(index++),
                Material.WRITABLE_BOOK,
                "<yellow>" + quest.name() + "</yellow>",
                List.of(
                    "<gray>ID: <gold>" + quest.id().value() + "</gold></gray>",
                    "<gray>Kategorie: <aqua>" + quest.metadata().category() + "</aqua></gray>",
                    "<gray>Typ: <green>" + quest.metadata().type().name() + "</green></gray>",
                    "<gray>Ziele: <yellow>" + quest.objectives().size() + "</yellow> | Belohnungen: <yellow>" + quest.rewards().size() + "</yellow></gray>",
                    quest.enabled() ? "<green>Aktiviert</green>" : "<red>Deaktiviert</red>",
                    "<yellow>Klicken zum Öffnen.</yellow>"
                ),
                "ADMIN_QUEST_INFO:" + quest.id().value()
            );
        }
    }

    private void fillNpcList(OpenMenu menu, List<Integer> slots) {
        int index = 0;
        for (NpcData npc : plugin.services().npcs().all()) {
            if (index >= slots.size()) {
                break;
            }
            addItem(
                menu,
                slots.get(index++),
                Material.VILLAGER_SPAWN_EGG,
                "<green>" + npc.displayName() + "</green>",
                List.of(
                    "<gray>ID: <gold>" + npc.id() + "</gold></gray>",
                    "<gray>Citizens: <yellow>" + (npc.citizensId().isPresent() ? npc.citizensId().getAsInt() : "-") + "</yellow></gray>",
                    "<gray>Quests: <yellow>" + npc.quests().size() + "</yellow> | Routenpunkte: <yellow>" + npc.routePoints().size() + "</yellow></gray>",
                    "<gray>Skin: <aqua>" + emptyFallback(npc.skin()) + "</aqua></gray>"
                ),
                "ADMIN_NPC_INFO:" + npc.id()
            );
        }
    }

    private void fillTitleList(OpenMenu menu, List<Integer> slots, List<PlayerQuestData> allData) {
        Set<String> titles = new LinkedHashSet<>();
        for (PlayerQuestData data : allData) {
            titles.addAll(data.unlockedTitles());
        }

        int index = 0;
        for (String title : titles) {
            if (index >= slots.size()) {
                break;
            }
            addItem(
                menu,
                slots.get(index++),
                Material.NAME_TAG,
                "<aqua>" + title + "</aqua>",
                List.of(
                    "<gray>Freigeschalteter Titel</gray>",
                    "<yellow>Klicken, um ihn dir zu geben.</yellow>"
                ),
                "ADMIN_GRANT_TITLE:" + title
            );
        }
    }

    private void addItem(OpenMenu menu, int slot, Material material, String name, List<String> lore, String action) {
        if (slot < 0 || slot >= menu.inventory().getSize()) {
            return;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(deserialize(name));
        if (!lore.isEmpty()) {
            meta.lore(lore.stream().map(this::deserialize).toList());
        }
        item.setItemMeta(meta);
        menu.inventory().setItem(slot, item);

        if (action != null && !action.isBlank()) {
            menu.holder().putButton(new GuiButton(slot, action));
        }
    }

    private Component deserialize(String value) {
        return miniMessage.deserialize(value == null ? "" : value);
    }

    private Material material(String materialName) {
        Material material = Material.matchMaterial(materialName == null ? "STONE" : materialName);
        return material == null ? Material.STONE : material;
    }

    private String questAction(String action, Quest quest) {
        if (action == null || action.isBlank()) {
            return "";
        }
        String normalized = action.toUpperCase(java.util.Locale.ROOT);
        if (normalized.equals("ACCEPT_QUEST") || normalized.equals("DECLINE_QUEST") || normalized.equals("CANCEL_QUEST") || normalized.equals("TRACK_QUEST")) {
            return action + ":" + quest.id().value();
        }
        return action;
    }

    private String questPlaceholders(Quest quest, String input) {
        String description = quest.description().isEmpty() ? "" : String.join(" ", quest.description());
        return (input == null ? "" : input)
            .replace("%quest_id%", quest.id().value())
            .replace("%quest_name%", quest.name())
            .replace("%quest_description%", description)
            .replace("%quest_category%", quest.metadata().category())
            .replace("%quest_type%", quest.metadata().type().name())
            .replace("%quest_difficulty%", quest.metadata().difficulty().name())
            .replace("%quest_duration%", emptyFallback(quest.metadata().estimatedDuration()));
    }

    private String emptyFallback(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private YamlConfiguration loadGui(String fileName) {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "gui/" + fileName));
    }

    private record OpenMenu(Inventory inventory, QuestMenuHolder holder) {
    }
}
