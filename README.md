# CraftplayQuests

CraftplayQuests ist ein modulares Quest-, RPG-, NPC-, Achievement- und Statistik-System für Paper- und Purpur-Server.

## Projektstatus

Aktueller Stand: Phase 1 bis Phase 9 sind als startfähiger Plugin-MVP umgesetzt. Der Kern ist gebaut und muss jetzt auf einem Paper-/Purpur-Testserver mit den optionalen Hook-Plugins geprüft werden.

- Maven-Projekt mit Java 21
- Paper-API als bereitgestellte Compile-Abhängigkeit
- Main-Klasse `de.craftplay.quests.CraftplayQuestsPlugin`
- `plugin.yml` mit Basisbefehl `/quests` und Aliasen `/quest`, `/cpquests`, `/q`, `/cpq`
- Grundrechte für Spieler und Administration
- First-Start-Bootstrap für Konfiguration, Sprache, GUI-Dateien, Speicherordner, Cache-Ordner, `lib`, `logs` und `temp`
- Automatische `server.yml` mit Server-ID, Installations-Token, Recovery-Code und API-Tokens
- Sprachsystem mit MiniMessage und UTF-8-Umlauten
- Vollständige Sprachdateien für `de_DE`, `en_US`, `fr_FR`, `es_ES`, `it_IT`, `nl_NL`, `pl_PL`, `tr_TR`, `ru_RU` und `pt_BR`
- Erste frei konfigurierbare GUI-YAML-Dateien
- MIT-Lizenz aus dem GitHub-Repository übernommen

## Aktueller Ausbau

Der aktuelle Stand ergänzt die zuvor offenen Integrationen für Titel, Advancements, Admin-GUI, PlaceholderAPI und API:

- Titelsystem mit auswählbaren freigeschalteten Titeln, laufendem TextDisplay über Spielern, View-Distance, Sneak-/Vanish-Ausblendung und sauberem Lifecycle beim Weltwechsel, Logout und Shutdown
- Advancement-System mit Datapack-Asset-Generierung, Bukkit-Load zur Laufzeit, Achievement-Awarding beim Freischalten und API-Registrierung zusätzlicher Advancement-Definitionen
- PlaceholderAPI-Integration als echte `%cpquests_*%`-Expansion mit `completed`, `active`, `points`, `reputation`, `achievements`, `titles`, `title`, `tracked`, `server`, `storage`, `quests_total` und `api_running`
- Admin-GUI mit isolierten Inventar-Aktionen pro geöffnetem Menü, Questliste, Questdetails, NPC-Liste, Titelübersicht, Titelvergabe sowie Basis-Erstellung von Quest-, NPC- und Titel-Datensätzen
- HTTP-API mit getrennten Token-Zwecken für `panel` und `homepage`, PlayerData-Endpunkten, Titel- und Achievement-Verwaltung, Advancement-Snapshot, Hook-Snapshot, Storage-Flush und Reload-Endpunkt

## Komplett Nachgezogene Restplanung

Der zweite Ausbau hat die offenen Planpunkte aus Phase 9 und Phase 10 in den Kern integriert:

- MySQL- und MariaDB-StorageProvider mit dynamisch geladenen JDBC-Treibern
- DirtyQueue mit Batch-Flush und konfigurierbarem Flush-Intervall
- Redis-Cache-/Sync-Service über Jedis-Reflection, optional und fehlertolerant
- Confirm-Code-System für gefährliche Admin-Aktionen
- Audit-Log unter `logs/audit.log`
- Cache-Service für Skin-/Head-Cache-Info und Cache-Löschung
- Export- und Backup-Service mit ZIP-Erzeugung unter `save/exports` und `save/backups`
- Persistentes NPC-System mit CPQ-ID, Citizens-ID, Skin, Standort, Quests und Routenpunkten
- Best-Effort-Importer für QuestsPlugin-YAMLs aus `import/questsplugin`
- StatsService mit gecachten Übersichten und Top-Spieler-Daten
- Advancement-Asset-Generator als Datapack-Struktur unter `save/exports/datapack`

## Phase 2A

Die technische Threading- und Service-Basis ist vorhanden:

- `ServiceRegistry` für zentrale Service-Initialisierung und sauberen Shutdown
- `AsyncTaskService` mit konfigurierbarer Worker-Anzahl über `performance.async-workers`
- `MainThreadService` als zentrale Brücke für spätere Bukkit-/Paper-Zugriffe auf dem Mainthread
- `TaskResult` und `TaskCallback` für async vorbereitete Arbeit mit Rückmeldung auf den Mainthread

## Phase 2B

Die Storage- und Library-Grundstruktur ist vorhanden:

- `StorageProvider`-Interface für dokumentenbasierte Speicheroperationen
- `StorageService` mit Provider-Auswahl über `storage.type`
- `YamlStorageProvider` für lokale YAML-Dokumente unter `save/yaml`
- `H2StorageProvider` als JDBC-Grundspeicher mit Dokumenttabelle
- `MySqlStorageProvider` und `MariaDbStorageProvider` für produktive Datenbanken
- DirtyQueue für zusammengefasste Schreibvorgänge
- Fallback auf YAML, wenn der konfigurierte Storage-Provider nicht initialisiert werden kann
- `LibraryLoaderService` mit konfigurierbarem Library-Cache unter `lib`
- Konfigurierbare Maven-Koordinaten für MySQL, MariaDB, H2 und Redis/Jedis
- `RedisCacheService` für optionalen Cache, Netzwerk-Sync und Leaderboard-Cache

## Phase 3A

Das Quest-Domain-Modell ist vorhanden:

- `Quest`, `QuestId`, `QuestMetadata` und `QuestCategory`
- `QuestType`, `QuestDifficulty` und `QuestStatus`
- `QuestObjective` mit `ObjectiveType`
- `QuestRequirement` mit `RequirementType`
- `QuestReward` mit `RewardType`
- Bukkit-freie Modelle, damit VersionAdapter und Hooks später sauber andocken können

## Phase 3B

Die Quest-Datenbasis und Service-Fassade sind vorhanden:

- `QuestApi` als gemeinsame Fassade für spätere Commands, GUIs und externe APIs
- `QuestService` mit Registry- und PlayerData-Zugriff
- `QuestRegistry` zum Laden, Speichern, Löschen und Auflisten von Quest-Dokumenten
- `QuestSerializer` für YAML-Dokumente
- `PlayerQuestData`, `PlayerQuestProgress` und `PlayerQuestDataRepository`
- `PlayerQuestDataSerializer` für aktive, abgeschlossene und verfolgte Quests

## Phase 4A

Die erste echte Laufzeitlogik ist vorhanden:

- `ObjectiveService` prüft Ziel-Fortschritt und erkennt abgeschlossene Quests
- `RequirementService` prüft interne Voraussetzungen wie aktiv, abgeschlossen, deaktiviert und benötigte Vorquests
- `RewardService` erstellt Reward-Pläne und validiert Belohnungsdaten grob
- `QuestService` nutzt Requirements beim Annehmen und Objectives beim Abschließen
- Fortschritt wird in `PlayerQuestData` immutable aktualisiert und gespeichert

## Direkt Mitgezogene Schritte

Zusätzlich zu Phase 4A sind fünf nächste Schritte bereits umgesetzt:

- Progress-Recording über `QuestApi#recordObjectiveProgress`
- Erste Bukkit-Listener für Block abbauen, Block platzieren, Mob töten und Fishing
- Automatische Quest-Abschlusserkennung nach Objective-Fortschritt
- Spielerbefehle für `/quests accept`, `/quests complete`, `/quests cancel`, `/quests track` und `/quests untrack`
- Automatischer Beispielquest-Seed `miner_story_01`, falls noch keine Quests existieren

## Phase 4B und 4C

Die Questlogik wurde erweitert:

- Interne Requirements für abgeschlossene Vorquests, Ruf, Titel und Achievements
- Mainthread-sichere externe Requirements für Permissions, Items und Placeholder
- Belohnungen für Questpunkte, Ruf, Titel, Achievements und Konsolenbefehle
- Item-Rewards über Bukkit-Materialien
- Vault-Geldbelohnungen über optionale Reflection, wenn Vault vorhanden ist
- LuckPerms-Permissions über Konsolenkommando-Fallback, wenn LuckPerms vorhanden ist
- HeadDatabase- und CustomItem-Rewards über Hook-/Command-Fallbacks
- Achievement-Freischaltung beim Questabschluss inklusive erstem Questabschluss

## Phase 5

Das Spieler- und Admin-GUI-System ist vorhanden:

- `GuiService` lädt Inventar-Menüs aus `gui/*.yml`
- `/quests` öffnet für Spieler das Hauptmenü
- Inventarklicks werden über `GuiListener` verarbeitet
- Questliste und Quest-Annahme sind über GUI-Aktionen angebunden
- GUI-Aktionen für Back, Close, Admin, Abenteuerbuch, Achievements, Track, Cancel, Player-Command, Console-Command, Message und Sound
- Admin-Menüs öffnen Quest-, NPC-, Kategorien-, Titel- und Einstellungsbereiche
- Questdetails werden aus der Admin-Questliste heraus geöffnet und Spieleraktionen werden mit Quest-ID ausgeführt
- Admin-GUI-Aktionen können Template-Quests, Template-NPCs und Admin-Titel erzeugen
- GUI-Klickaktionen werden pro geöffnetem Inventar gespeichert, damit parallele Admin-Sessions nicht kollidieren
- Bedrock-Erkennung über Floodgate ist vorbereitet, Chest-GUI kann später durch Forms ersetzt werden

## Phase 6

Die Integrations- und Versionsbasis ist vorhanden:

- Hook-Erkennung für Citizens, PlaceholderAPI, CMI, Jobs, HeadDatabase, Floodgate, LuckPerms und Vault
- Interne Placeholder-Ersetzung für aktive, abgeschlossene und verfolgte Questdaten
- PlaceholderAPI-Auswertung über Reflection, wenn PlaceholderAPI vorhanden ist
- Eigene PlaceholderAPI-Expansion `cpquests` mit Spieler-, Server-, Storage- und API-Placeholdern
- VersionAdapter-Erkennung für 1.21.x, 1.21.6+ und zukünftige 26.1.x-Familien
- Services für Daily-/Weekly-Reset-Cleanup beim Login, NPC-Verknüpfungen, Dialog-Angebote und Titelstatus
- Spieler können freigeschaltete Titel mit `/quests title <titel>` auswählen und mit `/quests title clear` entfernen
- Freigeschaltete Titel werden als TextDisplay über Spielern dargestellt, sofern die Serverversion TextDisplays unterstützt

## Phase 7

API-, Import- und externe Verwaltungsgrundlagen sind vorhanden:

- Lokale HTTP-API unter `/api/health`, `/api/stats/overview`, `/api/stats/top-players`, `/api/stats/player/{uuid}`, `/api/admin/quests`, `/api/admin/npcs`, `/api/admin/playerdata`, `/api/admin/titles`, `/api/admin/achievements`, `/api/admin/advancements`, `/api/admin/hooks`, `/api/admin/import`, `/api/admin/cache`, `/api/admin/export`, `/api/admin/storage/flush` und `/api/admin/reload`
- API ist standardmäßig deaktiviert und nutzt die Tokens aus `server.yml`; `homepage` darf Statistik-Endpunkte lesen, `panel` darf Admin-Endpunkte nutzen
- Konfiguration unter `api.enabled`, `api.host`, `api.port`, `api.rate-limit-per-minute` und `api.worker-threads`
- Quest-CRUD ist über YAML-Body und Token-Schutz angebunden
- NPC-List/Create/Delete sowie Link-, Skin- und Quest-Zuordnungsupdates sind über die API angebunden
- PlayerData kann über die API gelesen, gespeichert und zurückgesetzt werden
- Titel und Achievements können über die API vergeben, ausgewählt und aggregiert ausgelesen werden
- Advancement-Definitionen können über die API registriert und als Laufzeit-Snapshot ausgelesen werden
- Import-Service erzeugt Backup und Report für QuestsPlugin-Importläufe
- Server-ID, Storage, Hooks, Version, Questliste, Spielerstatistiken, Cache und Exports sind über die API auslesbar

## Phase 8

Admin-, Debug- und Inspect-Befehle sind vorhanden:

- `/quests list` listet geladene Quests
- `/quests info <questId>` zeigt Questdetails
- `/quests progress [player]` zeigt Spielerfortschritt
- `/quests debug hooks|storage|version|services` zeigt Laufzeitstatus
- `/quests import questsplugin` erzeugt einen Import-Report
- `/quests admin` und `/quests editor` öffnen das Admin-Menü
- `/quests give <player> <questId>` gibt einem Spieler eine Quest
- `/quests complete <player> <questId>` schließt eine Quest administrativ ab
- `/quests reset <player> <questId>` setzt eine Quest zurück
- `/quests cache info` und `/quests cache clear <skins|heads|all>` verwalten Cache-Daten
- `/quests confirm <code>` bestätigt gefährliche Aktionen
- `/quests export` und `/quests backup` erzeugen ZIP-Dateien
- `/quests npc ...` verwaltet CPQ-NPCs, Questzuordnung, Skin, Standort und Routenpunkte

## Phase 9

Die erste Testbasis ist vorhanden:

- JUnit 5 und Surefire sind eingerichtet
- Quest-Serializer-Roundtrip wird getestet
- Objective-Fortschritt und Completion-Erkennung werden getestet
- Requirement-Auswertung für Vorquests, Ruf, Titel und Achievements wird getestet
- PlayerQuestData-Serialisierung inklusive Punkte, Ruf, ausgewähltem Titel und Achievements wird getestet

## Build

```powershell
mvn package
```

Das fertige Plugin-JAR wird unter `target/CraftplayQuests-0.1.0-SNAPSHOT.jar` erzeugt.

## Zielplattform

- Java 21+
- Paper/Purpur
- Minecraft 1.21.x als aktuelle API-Basis
- Spätere Versionsunterschiede werden über ein Adapter-System gekapselt

## Noch zu Prüfen

Der Plugin-Kern ist jetzt gebaut. Vor einem produktiven Release fehlen vor allem echte Server-Smoke-Tests:

- Start auf Paper/Purpur mit Java 21
- Questannahme, Fortschritt, Abschluss, Speicherung und GUI-Klicks im Spiel
- Admin-GUI-Flows für Questdetails, NPC-Liste, Titelvergabe und Template-Erstellung
- Titel-TextDisplays im echten Mehrspielerbetrieb inklusive Sneak, Vanish, Weltwechsel und Logout
- H2-, YAML-, MySQL- und MariaDB-Fallbacks mit echten Plugin-Datenordnern
- Redis mit echtem Redis-Server
- Optionale Hook-Tests mit PlaceholderAPI-Expansion, Citizens, Floodgate, LuckPerms, Vault und HeadDatabase
- HTTP-API mit echten `server.yml`-Tokens
- Import von realen QuestsPlugin-Dateien
- Datapack-/Advancement-Dateien und Runtime-Awards im Serverbetrieb

## Entwicklungsregeln

- Keine festen Texte im Code
- Keine festen GUI-Slots im Code
- Minecraft-/Bukkit-Objekte nur auf dem Mainthread verändern
- Datenbank-, Datei-, Import- und Cache-Arbeit konsequent async ausführen
- GUI-Dateien liegen in `gui/`
- Sprachdateien liegen in `language/`
- Lokale Daten liegen unter `save/`
- Umlaute werden in Plugin-Ressourcen und README als UTF-8 gepflegt
- `.editorconfig` erzwingt UTF-8 als Projektstandard
- `.gitattributes` hält UTF-8-Dateien und LF-Zeilenenden im Repository stabil

## Nächster Schritt

Der Code ist jetzt für einen Server-Smoke-Test bereit. Die nächste Arbeit ist kein weiterer Planungsblock, sondern Testen auf einem echten Paper-/Purpur-Server mit Java 21 und den gewünschten Hook-Plugins.
