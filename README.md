# CraftplayQuests

CraftplayQuests ist ein modulares Quest-, RPG-, NPC-, Achievement- und Statistik-System für Paper- und Purpur-Server.

## Projektstatus

Aktueller Stand: Phase 1, Phase 2A, Phase 2B, Phase 3A und Phase 3B sind als technische Grundlage umgesetzt.

- Maven-Projekt mit Java 21
- Paper-API als bereitgestellte Compile-Abhängigkeit
- Main-Klasse `de.craftplay.quests.CraftplayQuestsPlugin`
- `plugin.yml` mit Basisbefehl `/quests` und Aliasen `/quest`, `/cpquests`, `/q`, `/cpq`
- Grundrechte für Spieler und Administration
- First-Start-Bootstrap für Konfiguration, Sprache, GUI-Dateien, Speicherordner, Cache-Ordner, `lib`, `logs` und `temp`
- Automatische `server.yml` mit Server-ID, Installations-Token, Recovery-Code und API-Tokens
- Sprachsystem mit MiniMessage und UTF-8-Umlauten
- Vollständige Sprachdateien für `de_DE`, `en_US`, `fr_FR`, `es_ES`, `it_IT`, `nl_NL`, `pl_PL`, `tr_TR`, `ru_RU` und `pt_BR`
- Sprachkeys für Commands, GUIs, Quests, Ziele, Voraussetzungen, Belohnungen, Admin, NPCs, Dialoge, Titel, Achievements, Storage, Libraries, Hooks, Bedrock, Cache, Confirm, API, Import, VersionAdapter und Fehlerfälle
- Erste frei konfigurierbare GUI-YAML-Dateien
- MIT-Lizenz aus dem GitHub-Repository übernommen

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
- Fallback auf YAML, wenn der konfigurierte Storage-Provider nicht initialisiert werden kann
- `LibraryLoaderService` mit konfigurierbarem Library-Cache unter `lib`
- Konfigurierbare Maven-Koordinaten für MySQL, MariaDB, H2 und Redis/Jedis
- Automatischer Download fehlender Libraries, wenn `libraries.auto-download` aktiviert ist

## Phase 3A

Das Quest-Domain-Modell ist vorhanden:

- `Quest`, `QuestId`, `QuestMetadata` und `QuestCategory`
- `QuestType`, `QuestDifficulty` und `QuestStatus`
- `QuestObjective` mit `ObjectiveType`
- `QuestRequirement` mit `RequirementType`
- `QuestReward` mit `RewardType`
- Grundvalidierung für Quest-IDs, Kategorien, Spieleranzahl, Ziele, Voraussetzungen und Belohnungen
- Bukkit-freie Modelle, damit VersionAdapter und Hooks später sauber andocken können

## Phase 3B

Die Quest-Datenbasis und Service-Fassade sind vorhanden:

- `QuestApi` als gemeinsame Fassade für spätere Commands, GUIs und externe APIs
- `QuestService` mit Registry- und PlayerData-Zugriff
- `QuestRegistry` zum Laden, Speichern, Löschen und Auflisten von Quest-Dokumenten
- `QuestSerializer` für YAML-Dokumente
- `PlayerQuestData` und `PlayerQuestProgress`
- `PlayerQuestDataRepository` mit Cache und Storage-Anbindung
- `PlayerQuestDataSerializer` für aktive, abgeschlossene und verfolgte Quests
- Basisoperationen für Quest annehmen, abschließen, abbrechen, verfolgen und nicht mehr verfolgen

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

## Bewusst Später

Diese Systeme sind noch nicht implementiert und werden später phasenweise ergänzt:

- Echte Gameplay-Fortschritte über Bukkit-/Paper-Events
- Vollständige Requirement-Auswertung über Hooks, Items, Permissions, Jobs, Placeholder und Fraktionen
- Vollständige Reward-Ausgabe mit Items, Geld, Commands, Permissions, Titeln und Achievements
- MySQL-/MariaDB-StorageProvider und Redis-CacheProvider als produktive Provider
- DirtyQueue, Batch-Saves und Storage-Migrationen
- Laufendes GUI-System mit Inventaröffnung und Klickverarbeitung
- PlaceholderAPI-Integration
- Citizens-NPC-System
- Floodgate-Bedrock-Forms
- LuckPerms-, Vault-, CMI-, Jobs- und HeadDatabase-Hooks
- Multi-Version-Adapter für 1.21.x und 26.1.x
- Daily-/Weekly-Reset
- NPC-Questrotation
- Dialogsystem
- Titel über TextDisplay
- Achievements und Advancements
- Teampanel- und Homepage-API
- Importer vom Quests-Plugin
- Unit-, Integrations- und Server-Smoke-Tests

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

## Nächste Phase

Phase 4 soll die erste echte Laufzeitlogik ergänzen:

- Objective-Service-Grundstruktur
- Requirement-Service-Grundstruktur
- Reward-Service-Grundstruktur
- Erste interne Validierung beim Annehmen und Abschließen von Quests
- Vorbereitung für Bukkit-Event-Listener ohne vollwertige Gameplay-Hooks
