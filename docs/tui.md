# MossMan TUI Reference

All MossMan interaction happens through Minecraft's chat using clickable, styled text — no GUI screens or external browser needed. The root command is `/mossman`.

---

## Conventions

- **`[clickable]`** — runs a command when clicked
- **`[suggest →]`** — opens a pre-filled command in the chat box
- **`[✎]`** — pencil link; suggests the update command pre-filled with the current value (editors only)
- **`[✗]`** — deletes or removes the item (requires appropriate permission)
- **Pagination** — all lists page at 10 items with `«` / `»` navigation links
- **SNBT patches** — update commands take a Minecraft SNBT compound, e.g. `{status:"DONE",priority:"HIGH"}`
- **Ticket keys** — formatted as `PREFIX-N`, e.g. `MOSS-1`
- **Duration format** — time is expressed as `1d 2h 30m` (1d = 24h); spaces optional

---

## Permissions

| Role | Can do |
|---|---|
| **VIEWER** | View projects and tickets |
| **CREATOR** | + Create tickets, comment, log time, watch |
| **EDITOR** | + Update tickets/projects, delete comments/logs |
| **ADMIN** | + Manage members, schema |
| **OWNER** | + Delete project, transfer ownership |

Projects also have an **external user permission** controlling what non-members can do (default: VIEWER or FORBID).

---

## Projects

### `/mossman project list [page]`
```
--- MossMan Projects ---
[MOSS] MossMan Mod Development
[TEST] Testing Workbench
« Page 1/2 »
[+ Create New Project]
```
`[PREFIX]` runs `/mossman project view PREFIX`. Only projects you have permission to view are shown.

---

### `/mossman project view <prefix>`
```
--- Project: [MOSS] ---
[✎] Ticket Prefix: MOSS
[✎] Name: MossMan Mod Development
[✎] Description: Core mod development project.
[✎] Icon Texture: None
[✎] Text Color: #3db86e
[✎] External Permission: VIEWER
Members: 2 [View]
Statuses: 4 [View]
Ticket Types: 3 [View]
Relationship Types: 1 [View]
[View Tickets]
```

---

### `/mossman project create <prefix> <displayName>`

Creates a new project. `prefix` is the short ticket key prefix (e.g. `MOSS`).

---

### `/mossman project update <prefix> <patch>`

Updates project fields via SNBT patch. Editable fields: `name`, `description`, `iconTexture`, `textColor`, `externalUserPermission`.

```
/mossman project update MOSS {description:"New description"}
```

---

### `/mossman project delete <prefix>`

Deletes the project. Owner or server op only.

---

## Members

### `/mossman project member list <prefix> [page]`
```
--- Members: MossMan Mod Development ---
[✎] Player123 (OWNER) — Bossman
[✎] Player456 (EDITOR) — Redstone Guy
[+ Add Member]
```

---

### `/mossman project member add <prefix> <player> <permission>`

Adds a player to the project. Permissions: `VIEWER`, `CREATOR`, `EDITOR`, `ADMIN`.

---

### `/mossman project member view <prefix> <player>`

Shows member details with `[✎]` edit links for `title` and `permission`.

---

### `/mossman project member update <prefix> <player> <patch>`

Updates `title` or `permission`.

```
/mossman project member update MOSS Player456 {permission:"ADMIN"}
```

---

### `/mossman project member remove <prefix> <player>`

Removes a member from the project.

---

### `/mossman project member transfer <prefix> <player>`

Transfers project ownership to another member.

---

## Project Schema

Editors can manage per-project statuses, ticket types, and relationship types. Each supports `list`, `add`, `view`, `update`, and `delete` subcommands.

```
/mossman project status list <prefix>
/mossman project status add <prefix> <key> <displayName>
/mossman project status view <prefix> <key>
/mossman project status update <prefix> <key> <patch>
/mossman project status delete <prefix> <key>
```

The same shape applies to `ticketType` and `relationshipType`.

Default statuses: `OPEN`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`
Default ticket types: `TASK`, `BUG`, `FEATURE`
Default relationship types: `BLOCKS`, `DUPLICATES`, `RELATES_TO`

---

## Tickets

### `/mossman ticket list <prefix> [page]`
```
--- Tickets: MOSS ---
[MOSS-1] Implement TUI commands [IN_PROGRESS]
[MOSS-2] Fix persistence bug [OPEN]
« Page 1/1 »
[+ Create Ticket]
```

Supports an optional SNBT filter compound:
```
/mossman ticket list MOSS {status:"OPEN",priority:"HIGH"}
/mossman ticket list MOSS {label:["bug","auth"]}
```
Filterable fields: `status`, `type`, `priority`, `title`, `label`.

---

### `/mossman ticket view <key>`
```
--- Ticket: MOSS-1 ---
[✎] Title: Implement TUI commands
[✎] Status: IN_PROGRESS
[✎] Labels: bug, auth
[✎] Description: Add interactive chat links.
Assignees: Player123 [✗]  Player456 [✗]
[+ Assign]
Observers: Player123  Player789
[✗ Unwatch]
Relationships:
  [✗] blocks → MOSS-2: Fix persistence bug
[+ Link]
Comments (3): [View all →]
  [✗] [#7] Player123 (02/25 14:30 EST): Wiring the command tree now.
  (2 older comments hidden)
[+ Comment]
Time Logged (3h 30m, 2 entries): [View all →]
  [✗] [#3] Player123 (02/25 15:00 EST): 1h 30m — Fixed auth bug
  (1 older entry hidden)
[+ Log Time]
[Edit]
```

---

### `/mossman ticket create <prefix> <title>`

Creates a ticket with default status/type/priority. Requires CREATOR+.

---

### `/mossman ticket update <key> <patch>`

Updates ticket fields via SNBT patch. Editable fields: `title`, `status`, `type`, `priority`, `description`, `labels`.

```
/mossman ticket update MOSS-1 {status:"DONE"}
/mossman ticket update MOSS-1 {title:"New title",priority:"HIGH"}
/mossman ticket update MOSS-1 {labels:["bug","auth"]}
```

---

### `/mossman ticket assign <key> <player>`
### `/mossman ticket unassign <key> <player>`

Assigns or unassigns a player. Requires EDITOR+. Assignee receives a mail notification.

---

### `/mossman ticket watch <key>`
### `/mossman ticket unwatch <key>`

Subscribe or unsubscribe from update notifications for a ticket. Creators are auto-added as observers on ticket creation; assignees are auto-added on assignment.

---

### `/mossman ticket link <sourceKey> <type> <targetKey>`
### `/mossman ticket unlink <sourceKey> <type> <targetKey>`

Creates or removes a typed relationship between two tickets. Available types are defined per-project under `relationshipType`.

```
/mossman ticket link MOSS-1 BLOCKS MOSS-2
```

---

## Comments

### `/mossman ticket comment <key> <message>`

Adds a comment. Requires CREATOR+. Observers (except the commenter) are notified via mail.

---

### `/mossman ticket comment delete <commentId>`

Deletes a comment. Authors can delete their own; EDITOR+ can delete any.

---

### `/mossman ticket comments <key> [page]`
```
--- Comments: MOSS-1 ---
  [✗] [#5] Player456 (02/25 12:00 EST): Looks good to me.
  [✗] [#7] Player123 (02/25 14:30 EST): Wiring the command tree now.
« Page 1/1 »
[← Back to ticket]
```

---

## Time Logging

### `/mossman ticket log <key> <duration> [note]`

Logs time spent on a ticket. Duration and optional note are passed as a single greedy string — duration tokens (`1d`, `2h`, `30m`) are parsed from the front; remaining text becomes the note.

```
/mossman ticket log MOSS-1 2h 30m Fixed auth bug
/mossman ticket log MOSS-1 1d
```

`1d = 24h`. Requires CREATOR+.

---

### `/mossman ticket log delete <logId>`

Deletes a time log entry. Authors can delete their own; EDITOR+ can delete any.

---

### `/mossman ticket logs <key> [page]`
```
--- Time Logs: MOSS-1 (3h 30m total) ---
  [✗] [#3] Player123 (02/25 15:00 EST): 1h 30m — Fixed auth bug
  [✗] [#2] Player456 (02/25 10:00 EST): 2h
« Page 1/1 »
[← Back to ticket]
```

---

## Mail

### `/mossman mail` or `/mossman mail list [page]`
```
--- Inbox (2 unread) ---
[✗] [#4] MossMan (02/25 09:00 EST): [MOSS-1] was updated  ●
[✗] [#3] Player456 (02/24 17:30 EST): Hey, can you review MOSS-2?  ●
    [#2] MossMan (02/23 11:00 EST): Assigned to [MOSS-3]
« Page 1/1 »
```
`●` marks unread messages. `[✗]` deletes the message.

---

### `/mossman mail read <id>`

Opens a message and marks it as read.

---

### `/mossman mail send <player> <subject> <message>`

Sends a mail message to another player.

---

## Notifications

MossMan sends automatic mail notifications for:

| Event | Recipients |
|---|---|
| Ticket assigned | Assignee |
| Ticket updated | All observers (except the requester) |
| Comment added | All observers (except the commenter) |

Players online at the time receive an in-chat whisper immediately. Players who join with unread messages see a login reminder with a `[View]` link.

---

## Settings

### `/mossman settings`

Shows current player settings (timezone, current local time).

---

### `/mossman settings timezone <zone>`

Sets your display timezone for all timestamps. Accepts any IANA timezone identifier (e.g. `America/New_York`, `Europe/London`, `UTC`). Tab-complete suggests common zones.

```
/mossman settings timezone America/New_York
/mossman settings timezone Asia/Tokyo
```

---

## Admin

### `/mossman admin db reset`

Drops and recreates all tables. **Destructive — deletes all data.** Requires server op.
