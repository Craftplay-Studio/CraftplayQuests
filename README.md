# CraftplayQuests

CraftplayQuests ist ein modulares Quest-, RPG-, NPC-, Achievement- und Statistik-System für Paper- und Purpur-Server.

## Projektstatus

Aktueller Stand: Phase 1 ist umgesetzt.

- Maven-Projekt mit Java 21
- Paper-API als bereitgestellte Compile-Abhängigkeit
- Main-Klasse `de.craftplay.quests.CraftplayQuestsPlugin`
- `plugin.yml` mit Basisbefehl `/quests` und Aliasen `/quest`, `/cpquests`, `/q`, `/cpq`
- Grundrechte für Spieler und Administration
- First-Start-Bootstrap für Konfiguration, Sprache, GUI-Dateien, Speicherordner, Cache-Ordner, `lib`, `logs` und `temp`
- Automatische `server.yml` mit Server-ID, Installations-Token, Recovery-Code und API-Tokens
- Sprachsystem mit MiniMessage und UTF-8-Umlauten
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
