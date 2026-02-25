# MossMan 🌿

MossMan is a modular, native project management mod for Minecraft, designed to bring agile workflows directly into your world. Built for Fabric 1.21.1, it provides a robust platform for tracking tickets, projects, and sprints without external browser dependencies in-game.

## 🚀 Key Features

- **Modular Core**: Designed as a foundational API for extensible project tracking.
- **Native UI**: High-performance, Minecraft-native chat TUI for all project interactions.
- **Agile Toolset**: Full support for Projects, Tickets, Sprints, and Relationships (Needs/Is Needed By).
- **Collaborative Permissions**: Granular roles (Owner, Admin, Editor, Creator, Viewer).
- **Observer System**: Get notified of updates to tickets you care about.
- **RESTful Design**: Built from the ground up to support future external integrations and web APIs.

## 📦 Requirements

- **Minecraft**: 1.21.1
- **Loader**: Fabric
- **Dependencies**: Fabric API

## 💬 In-Game TUI

All interaction happens through clickable, styled chat output — no GUI screens or external browser needed. The root command is `/mossman`.

Key commands at a glance:

| Area | Commands |
|---|---|
| Projects | `project list`, `project view`, `project create`, `project update` |
| Members | `project member list/add/view/update/remove/transfer` |
| Schema | `project status/ticketType/relationshipType list/add/view/update/delete` |
| Tickets | `ticket list`, `ticket view`, `ticket create`, `ticket update` |
| Workflow | `ticket assign/unassign`, `ticket watch/unwatch`, `ticket link/unlink` |
| Comments | `ticket comment`, `ticket comments`, `ticket comment delete` |
| Time | `ticket log`, `ticket logs`, `ticket log delete` |
| Mail | `mail`, `mail read`, `mail send` |
| Settings | `settings`, `settings timezone` |

Every view renders `[✎]` pencil links for editable fields (editors only), `[✗]` delete links, and `«»` pagination. Updates use Minecraft's SNBT compound format:

```
/mossman ticket update MOSS-1 {status:"DONE",priority:"HIGH"}
/mossman ticket log MOSS-1 2h 30m Fixed the auth bug
```

See **[docs/tui.md](docs/tui.md)** for the full command reference with output mockups.

## 🗺️ Roadmap

### MVP
- In-game GUI screen via `/mossman gui` (client-side, separate from chat TUI)
- SMP crafting/in-game item integration

### Post-MVP
- **Moss Monitor** — placeable block that displays MossMan data; linkable to other monitors via a config item; supports up to 8×8, must form a solid 1-wide rectangle (vertical or horizontal)
- **Moss Master 9000** — multiblock controller block for advanced configuration of linked monitor multiblocks

### Future
- Vanilla JS web app for browser access
- Trello / Jira integrations
- CSV, Excel, and pastebin exports
- String import/export for quick project copying
- Discord bot
- AI project planner

## 📜 Documentation

| Doc | Contents |
|---|---|
| [TUI Reference](docs/tui.md) | Full command reference with output mockups |
| [Architecture](.agent/architecture.md) | Layer diagram and event-driven flow |
| [Data Models](.agent/datamodels.md) | Field-level schema for all domain entities |
| [Coding Standards](.agent/standards.md) | API conventions and naming rules |
