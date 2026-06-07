# CraftplayQuests

CraftplayQuests ist ein modulares Quest-, RPG-, NPC-, Achievement- und Statistik-System für Paper- und Purpur-Server.

## Projektstatus

Aktueller Stand: Phase 1 ist umgesetzt und der vollständige Sprachkatalog für die geplanten Module ist vorbereitet.

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

- Echte Questlogik, Quest-Modell, Quest-Registry, Quest-API und Quest-Data
- Ziele, Voraussetzungen und Belohnungen mit Laufzeitlogik
- Storage-System mit YAML, H2, MySQL, MariaDB und Redis
- AsyncTaskService, MainThreadService, DirtyQueue und Batch-Saves
- LibraryLoaderService für Datenbank- und Redis-Libraries
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
- Datenbank-, Datei-, Import- und Cache-Arbeit später konsequent async ausführen
- GUI-Dateien liegen in `gui/`
- Sprachdateien liegen in `language/`
- Lokale Daten liegen unter `save/`
- Umlaute werden in Plugin-Ressourcen und README als UTF-8 gepflegt
- `.editorconfig` erzwingt UTF-8 als Projektstandard
- `.gitattributes` hält UTF-8-Dateien und LF-Zeilenenden im Repository stabil

## Nächste Phase

Phase 2 soll die technische Basis für Async- und Storage-Arbeit ergänzen:

- `AsyncTaskService`
- `MainThreadService`
- `StorageProvider`-Interface
- YAML-/H2-Grundspeicher
- `LibraryLoaderService`
