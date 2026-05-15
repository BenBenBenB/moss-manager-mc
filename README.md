# Role & Objective
You are an expert Minecraft Java mod developer specializing in Clean Architecture, multi-platform development using Architectury API (targeting 1.21+ for NeoForge and Fabric), and advanced UI design using ModularUI. 

Your objective is to generate the scaffolding, core logic, data layer, and presentation shells for a highly-extensible, decoupled "In-Game Project Management" mod. A template had been generated to support Fabric and NeoForge APIs.

---

# Architecture Requirements

### 1. Module Structure
Split the implementation into three distinct layers to ensure Clean Architecture principles:
*   `core/`: Pure Java/Kotlin. Zero dependencies on Minecraft, Architectury, or ModularUI. Contains the domain models, business logic (Use Cases), and Repository interfaces.
*   `api/`: Pure Java/Kotlin interface layer. Contains Event hooks, SPIs, and extension points that external mods (e.g., Jira connectors, Web servers) can implement or subscribe to.
*   `common/` (Architectury): Implementation layer. Implements the `core/` repository interfaces using Minecraft lifecycle events, handles networking packets, and hooks into ModularUI.
*   `fabric/` & `neoforge/`: Light wrapper modules handled via Architectury's standard platform configurations.

### 2. Domain Data Model (`core/`)
Implement immutable domain records/classes:
*   `AccessLevel`: Enum with values `NONE`, `VIEW`, `EDIT`, `ADMIN`.
*   `TicketStatus`: Enum with values `TODO`, `IN_PROGRESS`, `DONE`.
*   `Ticket`: Contains `id` (String/UUID), `title`, `description`, `assigneeUuid` (UUID), `status`, and `createdAt` (long).
*   `Project`: Contains `id` (String), `name`, `ownerUuid` (UUID), `globalAccess` (AccessLevel), `memberPermissions` (Map<UUID, AccessLevel>), and `tickets` (List<Ticket>).

### 3. Permissions & Business Logic (`core/`)
Create pure Java Use Cases that enforce the following permission rules:
*   The Project Owner inherently possesses `ADMIN` access.
*   If a player has explicit entry in `memberPermissions`, use their assigned `AccessLevel`.
*   If a player is not explicitly listed, fallback to the project's `globalAccess` setting.
*   Enforce permissions within business operations:
    *   `VIEW` required to read/list projects/tickets.
    *   `EDIT` required to create/modify tickets or change ticket status.
    *   `ADMIN` required to alter project settings, delete projects, or modify `memberPermissions`.

### 4. Storage Engine (`common/` -> Async JSON)
Implement a non-blocking, asynchronous file storage system inside the Architectury common module:
*   **Location:** Store everything inside the active world save directory under a subfolder path: `[world_root]/projectmanager/projects/[project_id].json`.
*   **Threading:** Utilize a dedicated, single-threaded executor service (`Executors.newSingleThreadExecutor()`) to process IO operations sequentially. This eliminates main-thread lag and avoids race conditions during file writes.
*   **Serialization:** Use GSON or Jackson (built into the environment) to cleanly map domain entities to formatted JSON text.
*   **Lifecycle Hook:** Register to Architectury’s `LifecycleEvent.SERVER_STARTED` to capture the world path dynamically via `server.getWorldPath(LevelResource.ROOT)` and initialize the directories. Load all active projects into an in-memory cache at startup to support instant cross-project querying (e.g., a global "My Tasks" view).

### 5. UI Presentation Shell (`common/` -> ModularUI)
Create the skeleton for the client-side user interface using ModularUI:
*   **Kanban Board View:** Implement a ModularUI `BaseScreen` that populates a horizontal `Row` layout. Inside this row, dynamically generate three vertical `ScrollPanel` or `List` columns mapping directly to the `TicketStatus` states (Todo, In Progress, Done).
*   **Ticket Cards:** Design a reusable widget structure for individual ticket previews displaying title, status, and assignment.
*   **Decoupled UI Layer:** Ensure the UI components remain completely separated from data operations. The UI must only interact with client-cached data copies and fire C2S networking packets via Architectury's `NetworkChannel` to request server-side mutations.

---

# Tasks to Complete
1. Generate the complete directory layout and project configurations for the `core`, `api`, and `common` modules.
2. Write the complete, production-ready Java code for the `core/` domain models, permission evaluators, and repository interfaces.
3. Write the asynchronous JSON persistence implementation in the `common/` module, including proper thread-pool lifecycle management.
4. Implement the network sync framework to push the server-side project cache down to clients.
5. Provide a clear template for a ModularUI Kanban screen mapping the synced client cache data.
