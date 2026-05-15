# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

An in-game project-management Minecraft mod (tickets, Kanban boards, per-project permissions). Multi-loader via **Architectury** targeting Minecraft 1.21.10 on **Fabric** and **NeoForge**. The full design spec lives in `README.md` — read it before starting feature work, especially the sections on Clean Architecture layering, the domain model (`Project`, `Ticket`, `AccessLevel`, `TicketStatus`), permission rules, the async JSON storage requirement, and the ModularUI Kanban UI.

The current code is still the freshly generated Architectury template (`ExampleMod`, `ExampleModFabric`, `ExampleModNeoForge`) with no domain logic yet — treat anything `Example*` as scaffolding to be renamed/replaced rather than as the production layout.

## Build & run

Java 21 is required (the build pins `JavaVersion.VERSION_21` and `options.release = 21`).

```bash
./gradlew build                    # builds common, fabric, and neoforge — produces release jars in <module>/build/libs/
./gradlew :fabric:runClient        # launches a dev Minecraft client with the Fabric build of the mod
./gradlew :neoforge:runClient      # same for NeoForge
./gradlew :fabric:runServer        # dev dedicated server
./gradlew clean                    # nuke all module build/ dirs
```

There are no tests wired up yet. When tests are added, run a single class with `./gradlew :common:test --tests com.mossman.SomeTest`.

A full clean build downloads ~1 GB of dependencies and takes ~3 minutes on a warm Gradle daemon; the first run is much slower because Loom remaps Minecraft and the mod libraries.

## Gradle / Loom gotchas

- **Loom version must be ≥ 1.14** in root `build.gradle`. The Architectury 1.21.10 template ships with `dev.architectury.loom version '1.11-SNAPSHOT'`, which fails `:neoforge` configuration with `NoSuchFileException: data/server.lzma` because newer NeoForge installers (21.10.63+) no longer ship `server.lzma`. We're pinned to `1.14-SNAPSHOT`.
- The Architectury Loom plugin is declared once in the root `build.gradle` (`apply false`) and applied to every subproject via the root `subprojects { apply plugin: 'dev.architectury.loom' }` block. Do not re-declare it inside `common/`, `fabric/`, or `neoforge/`.
- Dependencies are versioned in root `gradle.properties` (`minecraft_version`, `architectury_api_version`, `fabric_loader_version`, `fabric_api_version`, `neoforge_version`). Subproject `build.gradle` files reference these via `$rootProject.<name>`.

## Module layout (current)

- `common/` — Architectury common module. Code that targets the platform-agnostic API goes here. Entry point: `com.mossman.ExampleMod#init()`. Mixin config: `common/src/main/resources/moss_manager_mc.mixins.json`.
- `fabric/` — Fabric loader wrapper. Calls `ExampleMod.init()` from `ExampleModFabric#onInitialize`. Bundles `common`'s code into the final jar via the Shadow plugin (`shadowBundle` configuration → `transformProductionFabric`). Manifest: `fabric/src/main/resources/fabric.mod.json`.
- `neoforge/` — NeoForge loader wrapper, parallel structure to `fabric/`. Manifest: `neoforge/src/main/resources/META-INF/neoforge.mods.toml`.

The `${version}` placeholder in `fabric.mod.json` and `neoforge.mods.toml` is filled in by `processResources` from `mod_version` in `gradle.properties` — don't hardcode it.

### Module layout (planned per README)

The README calls for splitting `common/` further into `core/` (pure Java domain + use cases, zero Minecraft deps), `api/` (SPI / extension hooks for downstream mods), and `common/` (Architectury implementation of the `core/` repository interfaces, networking, ModularUI). When implementing, keep this boundary strict: anything in `core/` that imports a Minecraft, Architectury, or ModularUI type is a layering violation.

## Versioning & releases

- Single source of truth: `mod_version` in `gradle.properties`. Bump it on `main`, commit, then `git tag v<version> && git push origin v<version>`.
- `.github/workflows/release.yml` requires the tag to match `mod_version` and fails fast otherwise — do not push a tag without bumping the property first.
- `.github/workflows/build.yml` runs on every push and PR; it uploads the production Fabric and NeoForge jars as a workflow artifact (`jars-<sha>`).

## Mod ID

`moss_manager_mc` (defined as `MOD_ID` in `ExampleMod.java`, mirrored in both loader manifests and `archives_name` in `gradle.properties`). If renaming, update all five locations plus the mixin config filename.
