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
- Belohnungen für Questpunkte, Ruf, Titel, Achievements und Konsolenbefehle
- Item-Rewards über Bukkit-Materialien
- Vault-Geldbelohnungen über optionale Reflection, wenn Vault vorhanden ist
- LuckPerms-Permissions über Konsolenkommando-Fallback, wenn LuckPerms vorhanden ist
- Achievement-Freischaltung beim Questabschluss inklusive erstem Questabschluss

## Phase 5

Das Spieler-GUI-System ist vorhanden:

- `GuiService` lädt Inventar-Menüs aus `gui/*.yml`
- `/quests` öffnet für Spieler das Hauptmenü
- Inventarklicks werden über `GuiListener` verarbeitet
- Questliste und Quest-Annahme sind über GUI-Aktionen angebunden
- Bedrock-Erkennung über Floodgate ist vorbereitet, Chest-GUI kann später durch Forms ersetzt werden

## Phase 6

Die Integrations- und Versionsbasis ist vorhanden:

- Hook-Erkennung für Citizens, PlaceholderAPI, CMI, Jobs, HeadDatabase, Floodgate, LuckPerms und Vault
- Interne Placeholder-Ersetzung für aktive, abgeschlossene und verfolgte Questdaten
- PlaceholderAPI-Auswertung über Reflection, wenn PlaceholderAPI vorhanden ist
- VersionAdapter-Erkennung für 1.21.x, 1.21.6+ und zukünftige 26.1.x-Familien
- Services für Daily-/Weekly-Reset-Prüfung, NPC-Verknüpfungen, Dialog-Angebote und Titelstatus

## Phase 7

API-, Import- und externe Verwaltungsgrundlagen sind vorhanden:

- Lokale HTTP-API unter `/api/health`, `/api/stats/overview` und `/api/admin/quests`
- API ist standardmäßig deaktiviert und nutzt die Tokens aus `server.yml`
- Konfiguration unter `api.enabled`, `api.host`, `api.port` und `api.worker-threads`
- Import-Service erzeugt einen Report für spätere QuestsPlugin-Importläufe
- Server-ID, Storage, Hooks, Version und Questliste sind über die API auslesbar

## Phase 8

Admin-, Debug- und Inspect-Befehle sind vorhanden:

- `/quests list` listet geladene Quests
- `/quests info <questId>` zeigt Questdetails
- `/quests progress [player]` zeigt Spielerfortschritt
- `/quests debug hooks|storage|version|services` zeigt Laufzeitstatus
- `/quests import questsplugin` erzeugt einen Import-Report

## Phase 9

Die erste Testbasis ist vorhanden:

- JUnit 5 und Surefire sind eingerichtet
- Quest-Serializer-Roundtrip wird getestet
- Objective-Fortschritt und Completion-Erkennung werden getestet
- Requirement-Auswertung für Vorquests, Ruf, Titel und Achievements wird getestet
- PlayerQuestData-Serialisierung inklusive Punkte, Ruf, Titel und Achievements wird getestet

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
- H2- und YAML-Fallback mit echten Plugin-Datenordnern
- Optionale Hook-Tests mit PlaceholderAPI, Citizens, Floodgate, LuckPerms und Vault
- HTTP-API mit echten `server.yml`-Tokens
- Import-Report und spätere echte Import-Migrationen

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

Als Nächstes sollte Phase 4B folgen:

- Requirement-Hooks vorbereiten
- Reward-Ausführung für Commands und einfache interne Rewards
- Admin-/Debug-Kommandos für Quest- und PlayerData-Inspektion
- Erste Tests als Maven-Testquellen statt nur JShell-Smoke-Checks
