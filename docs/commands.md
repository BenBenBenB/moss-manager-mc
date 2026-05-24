project
- list
- view <projectId>
- create <projectId> <name>
- delete <projectId>
- update <projectId> <patch>
- config
    - role
        - list <projectId>
        - view <projectId> <roleId>
        - create <projectId> <roleId> [optional patch]
        - delete <projectId> <roleId>
        - update <projectId> <roleId> <patch>
        - order <projectId> <roleId> <position>
        - setpermission <projectId> <roleId> <permissionId> <GRANT|DENY|INHERIT>
        - assign <projectId> <roleId> <player>
        - unassign <projectId> <roleId> <player>
    - status
        - list <projectId>
        - view <projectId> <statusId>
        - create <projectId> <statusId> [optional patch]
        - delete <projectId> <statusId> [optional replacementStatusId]
        - update <projectId> <statusId> <patch>
        - order <projectId> <statusId> <position>
    - type
        - list <projectId>
        - view <projectId> <typeId>
        - create <projectId> <typeId> [optional patch]
        - delete <projectId> <typeId> [optional replacementTypeId]
        - update <projectId> <typeId> <patch>
        - order <projectId> <typeId> <position>

ticket
- list <projectId>
- view <projectId> <ticketId>
- create <projectId> [optional patch]
- delete <projectId> <ticketId>
- update <projectId> <ticketId> <patch>

## Notes

- **Default project template:** new projects (`project create`) copy roles, statuses, and types
  from `<world>/mossmandata/default.json`. The file is auto-created from a baked-in default on
  first server start and may be hand-edited. It is not loaded as a player-visible project.

- **Role priority & denials:** a project's `roles` list runs default at index 0 (always pinned)
  with higher indices having higher priority. Each role has separate grant and deny sets; when
  evaluating permissions for a player, the evaluator walks roles low→high and a later role's
  denial overrides an earlier role's grant.
  - `setpermission <projectId> <roleId> <permissionId> <level>` sets one permission on the role
    to one of `GRANT`, `DENY`, or `INHERIT`. The command rewrites grants and denials so that the
    permission ends up in exactly the chosen state; setting to `INHERIT` clears both. Errors if the
    permission is already at the target level.

- **Role order:** `position` is 1-based and must be in `[1, size-1]`. The default role is pinned
  at index 0 and cannot be reordered.

- **Status/type delete:** if the optional replacement is omitted, the first remaining
  status/type is auto-selected; affected tickets are reassigned.

- **Ticket numbering:** ticket numbers are issued by a project-level counter that only grows.
  Deleting the highest-numbered ticket does *not* release the number for reuse.

- **allowNonMembers update:** accepts SNBT boolean (`{allowNonMembers:true}` or
  `{allowNonMembers:false}`).
