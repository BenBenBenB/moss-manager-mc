# Moss Manager MC

An in-game project-management mod for Minecraft: tickets, Kanban boards, and
per-project permissions, all stored alongside the world save. Multi-loader via
[Architectury](https://github.com/architectury/architectury-api), targeting
Minecraft 1.21.10 on both Fabric and NeoForge.

## Status

Early development. The pure-Java domain core (`core/`) and extension SPI
(`api/`) are implemented and covered by tests. The Minecraft-facing pieces
(persistence, networking, ModernUI Kanban screen) are not started yet — the
`common/`, `fabric/`, and `neoforge/` modules are still the generated
Architectury template.

## Features (planned)

- Per-world projects with configurable tickets, statuses, types, and roles.
- Permission system: project owner, role-based permissions, and a global
  fallback for non-members.
- Async JSON storage under the active world save — no main-thread IO.
- ModernUI Kanban board, ticket dialogs, and admin panels.
- SPI hooks (`api/`) for downstream mods to react to project/ticket events.

## Requirements

- JDK 21 (the build pins `JavaVersion.VERSION_21` / `--release 21`).
- Minecraft 1.21.10 with either Fabric or NeoForge for runtime.

## Build & run

```bash
./gradlew build                    # build all modules; jars land in <module>/build/libs/
./gradlew :core:test               # run the pure-Java domain test suite
./gradlew :fabric:runClient        # dev Minecraft client on Fabric
./gradlew :neoforge:runClient      # dev Minecraft client on NeoForge
./gradlew :fabric:runServer        # dev dedicated server (Fabric)
./gradlew clean                    # remove all module build/ dirs
```

A clean build downloads ~1 GB of dependencies and takes ~3 minutes on a warm
Gradle daemon; the first run is slower because Loom remaps Minecraft.

## Project layout

- `core/` — pure Java domain model, use cases, and repository interfaces. No
  Minecraft, Architectury, or UI dependencies.
- `api/` — pure Java SPI: event types and listener interfaces for external mods
  to plug into (e.g. Jira sync, web dashboards).
- `common/` — Architectury common module. Implements `core/` repositories
  against the Minecraft lifecycle, owns networking and the ModernUI screens.
- `fabric/`, `neoforge/` — loader wrappers that bundle `common/` into a release
  jar via Shadow.

## Releases

`mod_version` in `gradle.properties` is the single source of truth. The release
workflow (`.github/workflows/release.yml`) requires a pushed tag `v<version>`
to match. The build workflow (`.github/workflows/build.yml`) runs on every
push and PR and uploads the Fabric and NeoForge jars as a `jars-<sha>`
artifact.

## Contributing

Issues and pull requests welcome. Before opening a PR:

- Run `./gradlew build` and `./gradlew :core:test` locally.
- Keep `core/` free of Minecraft, Architectury, and ModernUI imports — the
  layering is enforced by convention, not by the build (yet).

## License

TBD.
