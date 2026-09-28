# LastMark

LastMark remembers each player's most recent death location and can point a compass toward it. It is a small, local-only utility for Paper servers: no database, web service, or permissions setup is needed.

## Features

- Records the world and exact location when a player dies.
- Keeps one latest location per player across server restarts.
- Shows block coordinates with `/lastmark` (or `/lastdeath`).
- Points a held compass to that location with `/lastmark compass`.
- Lets a player remove their record with `/lastmark clear`.
- Writes saved data asynchronously and replaces the data file atomically where supported.

## Requirements

- Paper 26.2 or newer.
- Java 25 or newer for the server and build.

## Build

On Windows:

```powershell
.\gradlew.bat build
```

On macOS or Linux:

```sh
./gradlew build
```

The plugin JAR is written to `build/libs/lastmark-1.0.0.jar`. The Gradle wrapper downloads the required Gradle distribution the first time it runs.

## Install

1. Stop the Paper server.
2. Copy `lastmark-1.0.0.jar` into the server's `plugins` directory.
3. Start the server and confirm that `LastMark` appears in `/plugins`.

LastMark creates `plugins/LastMark/deaths.yml` automatically. Back up this file with your server backups; it contains player UUIDs and saved in-game locations.

## Use

After a player dies, their latest location is saved automatically.

```text
/lastmark          Show the saved world and block coordinates
/lastdeath         Alias for /lastmark
/lastmark compass  Point a held compass at the saved location
/lastmark clear    Delete your saved location
```

The compass command requires the recorded world to be loaded and the player to be in that same world. It checks both hands for a compass. The compass target is not tracked to a physical lodestone. Clearing the saved record does not alter a compass that was already marked; that compass keeps its last target until it is re-pointed or replaced.

## Data and privacy

All records stay in the server's `plugins/LastMark/deaths.yml` file. The file is keyed by player UUID and stores a world identifier, world name, coordinates, and the time recorded. LastMark does not contact external services.

## Compatibility

Built against the Paper API 26.2.129 and compiled for Java 25. It uses Bukkit-compatible plugin metadata (`plugin.yml`) and standard server APIs; it does not use NMS or experimental Paper plugin loading.
