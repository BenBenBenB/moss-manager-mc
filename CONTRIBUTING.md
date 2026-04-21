# Contributing to MossMan

## Prerequisites

### Java 21 JDK (required)

The project targets Java 21. You need a **JDK** (not a JRE), specifically version 21.

- **Linux (Debian/Ubuntu):** `sudo apt install openjdk-21-jdk`
- **macOS:** `brew install openjdk@21`
- **Windows / all platforms:** Download from [Adoptium](https://adoptium.net/temurin/releases/?version=21)

If you have multiple JDKs installed, Gradle will pick the right one automatically via the toolchain config (`languageVersion = JavaLanguageVersion.of(21)` in each subproject). You do **not** need to set `JAVA_HOME`.

> **Note:** Java 25+ JREs (runtime-only installs) will not work — `javac` is required.

### Other tools

- Git
- No other tools needed — the Gradle wrapper (`./gradlew`) downloads everything else.

---

## Project structure

This is a multi-module Gradle project. Each module produces a separate Fabric mod JAR.

| Module | Fabric mod ID | Description |
|---|---|---|
| `mossman-core` | `mossman` | Domain logic, database, infrastructure. Always required. |
| `mossman-tui` | `mossman-tui` | `/mossman` text commands. Optional — remove to disable commands. |
| `mossman-gui` | `mossman-gui` | GUI scaffold (work in progress). |

```
moss-manager-mc/
├── mossman-core/src/main/java/com/mossman/
│   ├── MossManMod.java          # Fabric entry point — initializes DB, repos, use cases
│   ├── MossManApi.java          # Public API — other modules call this, not MossManMod
│   ├── domain/                  # Entities, use cases, repository interfaces, events
│   └── infrastructure/          # OrmLite repositories, SQLite, config, event bus
├── mossman-tui/src/main/java/com/mossman/
│   ├── MossManTuiMod.java       # Fabric entry point — registers commands and event handlers
│   └── adapters/
│       ├── commands/            # Brigadier command handlers
│       └── tui/                 # Text formatting, suggestions, parsers
└── mossman-gui/src/main/java/com/mossman/
    └── MossManGuiMod.java       # Fabric client entry point (scaffold)
```

---

## Running locally

Because `mossman-tui` and `mossman-gui` depend on the `mossman-core` JAR at Gradle configuration time, **you must build core first** before running any dependent module.

### Client (singleplayer)

```bash
./gradlew :mossman-core:build
./gradlew :mossman-tui:runClient
```

### Server (multiplayer / headless)

```bash
./gradlew :mossman-core:build
./gradlew :mossman-tui:runServer
```

On first run you will be prompted to accept the Minecraft EULA. Edit `mossman-tui/run/eula.txt` and set `eula=true`.

### Running with the GUI module

```bash
./gradlew :mossman-core:build
./gradlew :mossman-gui:runClient
```

---

## Building release JARs

```bash
./gradlew :mossman-core:build
./gradlew :mossman-tui:build :mossman-gui:build
```

Output JARs land in each module's `build/libs/`:

- `mossman-core/build/libs/mossman-1.0.0.jar` — includes OrmLite + SQLite bundled via jar-in-jar
- `mossman-tui/build/libs/mossman-tui-1.0.0.jar`
- `mossman-gui/build/libs/mossman-gui-1.0.0.jar`

---

## Running tests

All domain and infrastructure tests live in `mossman-core`:

```bash
./gradlew :mossman-core:test
```

Tests use JUnit 5 and Mockito, and run without a Minecraft instance.

---

## Architecture

The project follows **clean architecture** with three layers enforced by module boundaries:

```
mossman-tui (adapter layer)
    └── depends on ↓
mossman-core
    ├── domain/      — pure Java: entities, use cases, repository interfaces, events
    └── infrastructure/ — OrmLite implementations, SQLite, config
```

**Rules to follow:**
- `domain/` must not import anything from `infrastructure/`, `adapters/`, or Minecraft.
- `domain/` may import `domain/util/` (e.g. `DurationParser`).
- Use cases talk to repositories through interfaces — never to OrmLite classes directly.
- Commands and TUI helpers access use cases and repositories through `MossManApi`, never by constructing them directly.
- `MossManApi` is initialized once by `MossManMod` — do not call `MossManApi.initialize()` from anywhere else.

---

## Adding a new UI module

Want to add an alternative interface (e.g. a web API, a Discord bot mod)? Depend on `mossman-core` and access everything through `MossManApi`:

1. Create a new Gradle subproject (e.g. `mossman-web/`).
2. Add it to `settings.gradle`: `include 'mossman-core', 'mossman-tui', 'mossman-gui', 'mossman-web'`
3. In `mossman-web/build.gradle`, declare: `modImplementation project(':mossman-core')`
4. Build core first before running your new module.
5. In your mod's `onInitialize()`, use `MossManApi.getXxx()` to access use cases and repositories, and `MossManApi.getEventBus()` to subscribe to domain events.

---

## IDE setup

**IntelliJ IDEA** is recommended. After cloning:

1. Open the root `build.gradle` as a project.
2. Let Gradle sync complete.
3. Run `./gradlew :mossman-core:build` once from the terminal so dependent modules resolve correctly.
4. Use the generated Loom run configurations (`runClient`, `runServer`) from the Gradle tool window under each subproject.
