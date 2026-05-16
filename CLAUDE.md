# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

An in-game project-management Minecraft mod (tickets, Kanban boards, per-project permissions). Multi-loader via **Architectury** targeting Minecraft 1.21.10 on **Fabric** and **NeoForge**. The user-facing overview lives in `README.md`; this file holds the full design spec and implementation guidance.

## Current state

- `core/` and `api/` are implemented and untracked-but-stable: domain model, permission evaluator, project/role/status/type/ticket use cases, plus the SPI event types. `./gradlew :core:test` runs 126 tests across 22 suites, all green.
- `common/` still contains only the generated `ExampleMod` scaffold — no wiring to `core/` yet. `fabric/` and `neoforge/` are unchanged template wrappers (`ExampleModFabric`, `ExampleModNeoForge`). Treat anything `Example*` as scaffolding to be renamed/replaced.
- Persistence, networking, and the ModernUI screens have not been started.

## Build & run

Java 21 is required (the build pins `JavaVersion.VERSION_21` and `options.release = 21`).

```bash
./gradlew build                    # builds all modules — release jars in <module>/build/libs/
./gradlew :core:test               # pure-Java domain test suite (fast, no MC remap)
./gradlew :fabric:runClient        # dev Minecraft client (Fabric)
./gradlew :neoforge:runClient      # dev Minecraft client (NeoForge)
./gradlew :fabric:runServer        # dev dedicated server
./gradlew clean                    # nuke all module build/ dirs
```

Run a single test class with `./gradlew :core:test --tests com.mossman.core.model.ProjectTest`.

A full clean build downloads ~1 GB of dependencies and takes ~3 minutes on a warm Gradle daemon; the first run is much slower because Loom remaps Minecraft and the mod libraries.

## Module layout

- `core/` — **pure Java**. Domain records (`Project`, `Ticket`, `Role`, `TicketStatus`, `TicketType`), permission evaluator, repository interfaces (`ProjectRepository`), and use cases under `usecase/{project,role,status,type,ticket}`. Tests live under `core/src/test/java`, with shared fixtures in `core/src/test/java/com/mossman/core/support/` (notably `InMemoryProjectRepository`). **Zero** Minecraft, Architectury, or ModernUI imports — any such import is a layering violation.
- `api/` — pure Java SPI for downstream mods. Event types in `api/event/` (`ProjectEvent`, `TicketEvent`) and listener interfaces in `api/spi/`. Same purity rule as `core/`.
- `common/` — Architectury common module. Will implement `core/`'s repository interfaces against the Minecraft lifecycle, own networking, and host ModernUI screens. Current entry point: `com.mossman.ExampleMod#init()`. Mixin config: `common/src/main/resources/moss_manager_mc.mixins.json`.
- `fabric/` — Fabric loader wrapper. Calls `ExampleMod.init()` from `ExampleModFabric#onInitialize`. Bundles `common/`'s code into the final jar via the Shadow plugin (`shadowBundle` configuration → `transformProductionFabric`). Manifest: `fabric/src/main/resources/fabric.mod.json`.
- `neoforge/` — NeoForge loader wrapper, parallel structure to `fabric/`. Manifest: `neoforge/src/main/resources/META-INF/neoforge.mods.toml`.

The `${version}` placeholder in `fabric.mod.json` and `neoforge.mods.toml` is filled in by `processResources` from `mod_version` in `gradle.properties` — don't hardcode it.

## Design spec

### Domain model (`core/`)

Domain types are immutable records/value objects. The canonical set:

- `Project` — `id`, `name`, `ownerUuid`, role membership (`Map<UUID, Role>`), the role catalog, status catalog, type catalog, and tickets.
- `Ticket` — `id`, `title`, `description`, `assigneeUuid`, `statusId`, `typeId`, `createdAt`.
- `Role` — named, ordered, colored, and carries a `RolePermissions` set. Roles are project-scoped; the project owner implicitly has all permissions.
- `TicketStatus`, `TicketType` — per-project, ordered, customizable. `Defaults` seeds the initial catalog so new projects come pre-populated (e.g. Todo / In Progress / Done).
- `Permission` — enum of granular capabilities checked by `PermissionEvaluator`.

If you ever see the older `AccessLevel { NONE, VIEW, EDIT, ADMIN }` enum referenced in old docs, that has been superseded by the `Role` + `Permission` model — don't reintroduce it.

### Permission rules

`PermissionEvaluator` is the single source of truth. The rules it enforces:

- The project owner inherently has every permission, regardless of role assignments.
- A player listed in the project's role membership uses the permissions granted by their `Role`.
- A player not explicitly listed falls back to project-level visibility settings (e.g. `allowNonMembers`).
- Use cases must call the evaluator before mutating state; the corresponding exceptions are `PermissionDeniedException`, `ValidationException`, and `NotFoundException` under `core/usecase/`.

### Storage engine (`common/` — async JSON)

To be implemented in `common/`. Requirements:

- **Location:** under the active world save, at `[world_root]/projectmanager/projects/[project_id].json`.
- **Threading:** a dedicated single-threaded executor (`Executors.newSingleThreadExecutor()`) so all IO is serialized off the main thread — no main-thread lag, no write races.
- **Serialization:** GSON or Jackson, formatted JSON, mapping straight to the `core/` records.
- **Lifecycle:** hook Architectury's `LifecycleEvent.SERVER_STARTED` to resolve the world path via `server.getWorldPath(LevelResource.ROOT)`, initialize directories, and load all projects into the in-memory cache so cross-project queries (e.g. a global "My Tasks" view) are instant.

### UI presentation shell (`common/` — ModernUI)

To be implemented in `common/` using **ModernUI** (not ModularUI — earlier drafts mentioned the wrong library):

- **Kanban board:** horizontal layout with one vertical scrollable column per `TicketStatus` in the project's status catalog (so the columns are data-driven, not hardcoded to three).
- **Ticket cards:** reusable widget showing title, status, type, and assignee.
- **Decoupling:** the UI reads only from a client-side cache and issues C2S packets via Architectury's `NetworkChannel` for mutations. No direct repository calls from UI code.

### Networking

Server is authoritative. Clients hold a synced read-only cache and request changes via packets that fan out to the matching `core/` use case. Server-side mutations broadcast deltas to interested clients.

## Gradle / Loom gotchas

- **Loom version must be ≥ 1.14** in root `build.gradle`. The Architectury 1.21.10 template ships with `dev.architectury.loom version '1.11-SNAPSHOT'`, which fails `:neoforge` configuration with `NoSuchFileException: data/server.lzma` because newer NeoForge installers (21.10.63+) no longer ship `server.lzma`. We're pinned to `1.14-SNAPSHOT`.
- Loom is applied **only** to the Minecraft-aware modules (`common`, `fabric`, `neoforge`) via the `configure(subprojects.findAll { it.name in [...] })` block in root `build.gradle`. `core/` and `api/` are pure-Java subprojects and must stay that way — do not apply Loom or Architectury plugins to them.
- The root `subprojects { apply plugin: 'java' }` block sets up Java 21 for every module including `core` and `api`. Do not re-declare Loom inside `common/`, `fabric/`, or `neoforge/`.
- Dependencies are versioned in root `gradle.properties` (`minecraft_version`, `architectury_api_version`, `fabric_loader_version`, `fabric_api_version`, `neoforge_version`). Subproject `build.gradle` files reference these via `$rootProject.<name>`.

## Versioning & releases

- Single source of truth: `mod_version` in `gradle.properties`. Bump it on `main`, commit, then `git tag v<version> && git push origin v<version>`.
- `.github/workflows/release.yml` requires the tag to match `mod_version` and fails fast otherwise — do not push a tag without bumping the property first.
- `.github/workflows/build.yml` runs on every push and PR; it uploads the production Fabric and NeoForge jars as a workflow artifact (`jars-<sha>`).

## Mod ID

`moss_manager_mc` (defined as `MOD_ID` in `ExampleMod.java`, mirrored in both loader manifests and `archives_name` in `gradle.properties`). If renaming, update all five locations plus the mixin config filename.
