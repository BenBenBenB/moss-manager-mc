package com.mossman.gui.screens;

import com.mossman.MossManApi;
import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Project;
import com.mossman.domain.entities.Status;
import com.mossman.domain.entities.TicketType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ProjectSettingsScreen extends Screen {

    private static final int HEADER_HEIGHT = 32;
    private static final int TAB_BAR_HEIGHT = 24;
    private static final int FOOTER_HEIGHT = 36;
    private static final int CONTENT_TOP = HEADER_HEIGHT + TAB_BAR_HEIGHT;

    private final Screen parent;
    private Project project;
    private final Permission playerPermission;

    private int currentTab = 0;
    private String errorMessage;

    // General tab fields (persisted across rebuilds)
    private String genName;
    private String genDesc;
    private String genColor;
    private Permission genExtPerm;
    private boolean genInitialized = false;

    // Statuses tab working list
    private List<Status> workingStatuses;
    private boolean statusesInitialized = false;

    // Ticket types tab working list
    private List<TicketType> workingTypes;
    private boolean typesInitialized = false;

    // Add-form fields (cleared on save/tab change)
    private String addKey = "";
    private String addDisplay = "";
    private String addColor = "";
    private String addMemberName = "";
    private Permission addMemberPerm = Permission.VIEWER;

    public ProjectSettingsScreen(Screen parent, Project project) {
        super(Text.literal("Settings: " + project.getName()));
        this.parent = parent;
        this.project = project;

        UUID playerId = null;
        if (MinecraftClient.getInstance().player != null) {
            playerId = MinecraftClient.getInstance().player.getUuid();
        }
        Permission perm = Permission.VIEWER;
        if (playerId != null) {
            final UUID pid = playerId;
            Optional<Member> member = project.getMembers().stream()
                    .filter(m -> m.uuid().equals(pid))
                    .findFirst();
            perm = member.map(Member::permission).orElse(project.getExternalUserPermission());
        }
        this.playerPermission = perm;
    }

    @Override
    protected void init() {
        errorMessage = null;

        // Initialize working state once
        if (!genInitialized) {
            genName = project.getName();
            genDesc = project.getDescription() != null ? project.getDescription() : "";
            genColor = project.getTextColor() != null ? project.getTextColor() : "";
            genExtPerm = project.getExternalUserPermission();
            genInitialized = true;
        }
        if (!statusesInitialized) {
            workingStatuses = new ArrayList<>(project.getStatuses());
            statusesInitialized = true;
        }
        if (!typesInitialized) {
            workingTypes = new ArrayList<>(project.getTicketTypes());
            typesInitialized = true;
        }

        // Tab buttons
        addDrawableChild(ButtonWidget.builder(Text.literal("General"), btn -> switchTab(0))
                .dimensions(width / 2 - 206, HEADER_HEIGHT + 2, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Members"), btn -> switchTab(1))
                .dimensions(width / 2 - 102, HEADER_HEIGHT + 2, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Statuses"), btn -> switchTab(2))
                .dimensions(width / 2 + 2, HEADER_HEIGHT + 2, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Ticket Types"), btn -> switchTab(3))
                .dimensions(width / 2 + 106, HEADER_HEIGHT + 2, 100, 20).build());

        switch (currentTab) {
            case 0 -> initGeneralTab();
            case 1 -> initMembersTab();
            case 2 -> initStatusesTab();
            case 3 -> initTypesTab();
        }

        // Footer
        int footerY = height - 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> close())
                .dimensions(width / 2 - 50, footerY, 100, 20).build());
    }

    private void initGeneralTab() {
        UUID playerId = client.player != null ? client.player.getUuid() : null;
        boolean canAdmin = playerPermission.ordinal() >= Permission.ADMIN.ordinal();

        int fieldW = 200;
        int fieldX = width / 2 - fieldW / 2;
        int y = CONTENT_TOP + 8;

        TextFieldWidget nameField = new TextFieldWidget(textRenderer, fieldX, y, fieldW, 20, Text.empty());
        nameField.setMaxLength(64);
        nameField.setText(genName);
        nameField.setChangedListener(t -> { genName = t; errorMessage = null; });
        nameField.setEditable(canAdmin);
        addDrawableChild(nameField);
        y += 28;

        TextFieldWidget descField = new TextFieldWidget(textRenderer, fieldX, y, fieldW, 20, Text.empty());
        descField.setMaxLength(256);
        descField.setText(genDesc);
        descField.setChangedListener(t -> { genDesc = t; errorMessage = null; });
        descField.setEditable(canAdmin);
        addDrawableChild(descField);
        y += 28;

        TextFieldWidget colorField = new TextFieldWidget(textRenderer, fieldX, y, fieldW, 20, Text.empty());
        colorField.setMaxLength(7);
        colorField.setPlaceholder(Text.literal("#RRGGBB"));
        colorField.setText(genColor);
        colorField.setChangedListener(t -> { genColor = t; errorMessage = null; });
        colorField.setEditable(canAdmin);
        addDrawableChild(colorField);
        y += 28;

        if (canAdmin) {
            addDrawableChild(CyclingButtonWidget.<Permission>builder(p -> Text.literal(p.name()), genExtPerm)
                    .values(Permission.FORBID, Permission.VIEWER, Permission.CREATOR, Permission.EDITOR, Permission.ADMIN)
                    .build(fieldX, y, fieldW, 20, Text.literal("External Access"),
                            (btn, val) -> genExtPerm = val));
            y += 28;

            addDrawableChild(ButtonWidget.builder(Text.literal("Save General"), btn -> trySaveGeneral(playerId))
                    .dimensions(fieldX, y, fieldW, 20).build());
        }
    }

    private void initMembersTab() {
        UUID playerId = client.player != null ? client.player.getUuid() : null;
        boolean canAdmin = playerPermission.ordinal() >= Permission.ADMIN.ordinal();
        boolean isOwner = playerPermission == Permission.OWNER;

        List<Member> members = project.getMembers();

        // Add form at top (if ADMIN+)
        int addFormY = CONTENT_TOP + 4;
        if (canAdmin) {
            TextFieldWidget memberNameField = new TextFieldWidget(textRenderer, 10, addFormY, 140, 20, Text.empty());
            memberNameField.setMaxLength(64);
            memberNameField.setPlaceholder(Text.literal("Player name (online)"));
            memberNameField.setText(addMemberName);
            memberNameField.setChangedListener(t -> addMemberName = t);
            addDrawableChild(memberNameField);

            addDrawableChild(CyclingButtonWidget.<Permission>builder(p -> Text.literal(p.name()), addMemberPerm)
                    .values(Permission.VIEWER, Permission.CREATOR, Permission.EDITOR, Permission.ADMIN)
                    .build(154, addFormY, 100, 20, Text.literal("Permission"),
                            (btn, val) -> addMemberPerm = val));

            addDrawableChild(ButtonWidget.builder(Text.literal("Add"), btn -> tryAddMember(playerId))
                    .dimensions(258, addFormY, 50, 20).build());
        }

        int listTop = CONTENT_TOP + (canAdmin ? 28 : 0);
        int listBottom = isOwner ? height - FOOTER_HEIGHT - 28 : height - FOOTER_HEIGHT;
        memberListWidget = new MemberListWidget(
                client, width, listBottom - listTop, listTop, 28, members, playerId, canAdmin);
        addDrawableChild(memberListWidget);

        // Transfer ownership button (OWNER only)
        if (isOwner) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Transfer Ownership..."),
                    btn -> promptTransfer(playerId))
                    .dimensions(width / 2 - 100, height - FOOTER_HEIGHT - 24, 200, 20).build());
        }
    }

    private void initStatusesTab() {
        UUID playerId = client.player != null ? client.player.getUuid() : null;
        boolean canEdit = playerPermission.ordinal() >= Permission.EDITOR.ordinal();

        // Add form
        int addY = CONTENT_TOP + 4;
        if (canEdit) {
            TextFieldWidget keyField = new TextFieldWidget(textRenderer, 10, addY, 80, 20, Text.empty());
            keyField.setMaxLength(32);
            keyField.setPlaceholder(Text.literal("KEY"));
            keyField.setText(addKey);
            keyField.setChangedListener(t -> addKey = t);
            addDrawableChild(keyField);

            TextFieldWidget dispField = new TextFieldWidget(textRenderer, 94, addY, 110, 20, Text.empty());
            dispField.setMaxLength(64);
            dispField.setPlaceholder(Text.literal("Display Name"));
            dispField.setText(addDisplay);
            dispField.setChangedListener(t -> addDisplay = t);
            addDrawableChild(dispField);

            TextFieldWidget colorField = new TextFieldWidget(textRenderer, 208, addY, 70, 20, Text.empty());
            colorField.setMaxLength(7);
            colorField.setPlaceholder(Text.literal("#RRGGBB"));
            colorField.setText(addColor);
            colorField.setChangedListener(t -> addColor = t);
            addDrawableChild(colorField);

            addDrawableChild(ButtonWidget.builder(Text.literal("Add"), btn -> {
                if (!addKey.isBlank() && !addDisplay.isBlank()) {
                    workingStatuses.add(new Status(addKey.toUpperCase().trim(), addDisplay.trim(), addColor.trim()));
                    addKey = ""; addDisplay = ""; addColor = "";
                    switchTab(2);
                }
            }).dimensions(282, addY, 40, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Save All"), btn -> trySaveStatuses(playerId))
                    .dimensions(width - 90, addY, 80, 20).build());
        }

        int listTop = CONTENT_TOP + (canEdit ? 28 : 0);
        statusListWidget = new StatusListWidget(
                client, width, height - listTop - FOOTER_HEIGHT, listTop, 24, workingStatuses, canEdit);
        addDrawableChild(statusListWidget);
    }

    private void initTypesTab() {
        UUID playerId = client.player != null ? client.player.getUuid() : null;
        boolean canEdit = playerPermission.ordinal() >= Permission.EDITOR.ordinal();

        int addY = CONTENT_TOP + 4;
        if (canEdit) {
            TextFieldWidget keyField = new TextFieldWidget(textRenderer, 10, addY, 80, 20, Text.empty());
            keyField.setMaxLength(32);
            keyField.setPlaceholder(Text.literal("KEY"));
            keyField.setText(addKey);
            keyField.setChangedListener(t -> addKey = t);
            addDrawableChild(keyField);

            TextFieldWidget dispField = new TextFieldWidget(textRenderer, 94, addY, 110, 20, Text.empty());
            dispField.setMaxLength(64);
            dispField.setPlaceholder(Text.literal("Display Name"));
            dispField.setText(addDisplay);
            dispField.setChangedListener(t -> addDisplay = t);
            addDrawableChild(dispField);

            TextFieldWidget colorField = new TextFieldWidget(textRenderer, 208, addY, 70, 20, Text.empty());
            colorField.setMaxLength(7);
            colorField.setPlaceholder(Text.literal("#RRGGBB"));
            colorField.setText(addColor);
            colorField.setChangedListener(t -> addColor = t);
            addDrawableChild(colorField);

            addDrawableChild(ButtonWidget.builder(Text.literal("Add"), btn -> {
                if (!addKey.isBlank() && !addDisplay.isBlank()) {
                    workingTypes.add(new TicketType(addKey.toUpperCase().trim(), addDisplay.trim(), addColor.trim()));
                    addKey = ""; addDisplay = ""; addColor = "";
                    switchTab(3);
                }
            }).dimensions(282, addY, 40, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Save All"), btn -> trySaveTypes(playerId))
                    .dimensions(width - 90, addY, 80, 20).build());
        }

        int listTop = CONTENT_TOP + (canEdit ? 28 : 0);
        typeListWidget = new TypeListWidget(
                client, width, height - listTop - FOOTER_HEIGHT, listTop, 24, workingTypes, canEdit);
        addDrawableChild(typeListWidget);
    }

    private void switchTab(int tab) {
        currentTab = tab;
        // Reset add-form fields on tab change
        addKey = ""; addDisplay = ""; addColor = "";
        this.init(this.width, this.height);
    }

    private void trySaveGeneral(UUID playerId) {
        if (playerId == null) return;
        try {
            Map<String, String> patch = new HashMap<>();
            patch.put("name", genName);
            patch.put("description", genDesc);
            patch.put("textColor", genColor);
            patch.put("externalUserPermission", genExtPerm.name());
            project = MossManApi.getUpdateProjectUseCase().execute(project.getId(), playerId, patch);
            genInitialized = false;
            switchTab(0);
        } catch (Exception e) {
            errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }
    }

    private void trySaveStatuses(UUID playerId) {
        if (playerId == null) return;
        try {
            project = MossManApi.getUpdateProjectStatusesUseCase().execute(project.getId(), playerId, workingStatuses);
            statusesInitialized = false;
            switchTab(2);
        } catch (Exception e) {
            errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }
    }

    private void trySaveTypes(UUID playerId) {
        if (playerId == null) return;
        try {
            project = MossManApi.getUpdateProjectTicketTypesUseCase().execute(project.getId(), playerId, workingTypes);
            typesInitialized = false;
            switchTab(3);
        } catch (Exception e) {
            errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }
    }

    private void tryAddMember(UUID playerId) {
        if (playerId == null || addMemberName.isBlank()) return;
        UUID newMemberId = resolvePlayerUuid(addMemberName.trim());
        if (newMemberId == null) {
            errorMessage = "Player '" + addMemberName.trim() + "' not found (must be online)";
            return;
        }
        try {
            Member newMember = new Member(0, newMemberId, addMemberName.trim(), "", addMemberPerm);
            project = MossManApi.getAddMemberUseCase().execute(project.getId(), playerId, newMember);
            addMemberName = "";
            switchTab(1);
        } catch (Exception e) {
            errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }
    }

    private void promptTransfer(UUID playerId) {
        // Simple: use a text input approach by composing another screen or inline
        // For simplicity, show error if no valid target; advanced flow: open a selection screen
        errorMessage = "To transfer ownership, use /mossman admin transfer-ownership <project> <player>";
    }

    private void tryRemoveMember(UUID playerId, UUID targetId) {
        if (playerId == null) return;
        try {
            project = MossManApi.getRemoveMemberUseCase().execute(project.getId(), playerId, targetId);
            switchTab(1);
        } catch (Exception e) {
            errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }
    }

    private UUID resolvePlayerUuid(String name) {
        if (client.getNetworkHandler() == null) return null;
        return client.getNetworkHandler().getPlayerList().stream()
                .filter(p -> p.getProfile().name().equalsIgnoreCase(name))
                .map(p -> p.getProfile().id())
                .findFirst()
                .orElse(null);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);

        // Tab underline
        int[] tabX = {width / 2 - 206, width / 2 - 102, width / 2 + 2, width / 2 + 106};
        int underlineY = HEADER_HEIGHT + TAB_BAR_HEIGHT - 2;
        context.fill(tabX[currentTab], underlineY, tabX[currentTab] + 100, underlineY + 2, 0xFFFFFFFF);

        // Label above General fields
        if (currentTab == 0) {
            int fieldX = width / 2 - 100;
            int y = CONTENT_TOP + 8;
            context.drawTextWithShadow(textRenderer, Text.literal("Name"), fieldX, y - 10, 0xAAAAAA);
            y += 28;
            context.drawTextWithShadow(textRenderer, Text.literal("Description"), fieldX, y - 10, 0xAAAAAA);
            y += 28;
            context.drawTextWithShadow(textRenderer, Text.literal("Text Color"), fieldX, y - 10, 0xAAAAAA);
            y += 28;
            context.drawTextWithShadow(textRenderer, Text.literal("External Access"), fieldX, y - 10, 0xAAAAAA);
        }

        if (errorMessage != null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(errorMessage),
                    width / 2, height - FOOTER_HEIGHT - 12, 0xFF4444);
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private MemberListWidget memberListWidget;
    private StatusListWidget statusListWidget;
    private TypeListWidget typeListWidget;

    @Override
    public boolean mouseClicked(Click click, boolean active) {
        if (click.button() == 0) {
            double mouseX = click.x();
            double mouseY = click.y();
            if (currentTab == 1 && memberListWidget != null) {
                MemberListWidget.MemberEntry entry = memberListWidget.getEntryAt(mouseX, mouseY);
                UUID playerId = client.player != null ? client.player.getUuid() : null;
                if (entry != null && entry.canAdmin && entry.member.permission() != Permission.OWNER && playerId != null) {
                    int delX = entry.getX() + memberListWidget.getRowWidth() - 60;
                    if (mouseX >= delX && mouseX <= delX + 56
                            && mouseY >= entry.getY() + 4 && mouseY <= entry.getY() + 22) {
                        tryRemoveMember(playerId, entry.member.uuid());
                        return true;
                    }
                }
            } else if (currentTab == 2 && statusListWidget != null) {
                StatusListWidget.StatusEntry entry = statusListWidget.getEntryAt(mouseX, mouseY);
                if (entry != null && entry.canEdit) {
                    int delX = entry.getX() + statusListWidget.getRowWidth() - 60;
                    if (mouseX >= delX && mouseX <= delX + 56
                            && mouseY >= entry.getY() + 2 && mouseY <= entry.getY() + 20) {
                        workingStatuses.remove(entry.index);
                        switchTab(2);
                        return true;
                    }
                }
            } else if (currentTab == 3 && typeListWidget != null) {
                TypeListWidget.TypeEntry entry = typeListWidget.getEntryAt(mouseX, mouseY);
                if (entry != null && entry.canEdit) {
                    int delX = entry.getX() + typeListWidget.getRowWidth() - 60;
                    if (mouseX >= delX && mouseX <= delX + 56
                            && mouseY >= entry.getY() + 2 && mouseY <= entry.getY() + 20) {
                        workingTypes.remove(entry.index);
                        switchTab(3);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(click, active);
    }

    // --- Member list widget ---

    private class MemberListWidget extends AlwaysSelectedEntryListWidget<MemberListWidget.MemberEntry> {

        public MemberListWidget(MinecraftClient client, int width, int height, int y,
                                int itemHeight, List<Member> members, UUID playerId, boolean canAdmin) {
            super(client, width, height, y, itemHeight);
            for (Member m : members) {
                addEntry(new MemberEntry(m, playerId, canAdmin));
            }
        }

        @Override
        public int getRowWidth() {
            return ProjectSettingsScreen.this.width - 20;
        }

        @Override
        protected int getScrollbarX() {
            return ProjectSettingsScreen.this.width / 2 + getRowWidth() / 2 + 4;
        }

        public MemberEntry getEntryAt(double x, double y) {
            return getEntryAtPosition(x, y);
        }

        public class MemberEntry extends AlwaysSelectedEntryListWidget.Entry<MemberEntry> {
            final Member member;
            final UUID playerId;
            final boolean canAdmin;

            public MemberEntry(Member member, UUID playerId, boolean canAdmin) {
                this.member = member;
                this.playerId = playerId;
                this.canAdmin = canAdmin;
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                String row = member.username() + "  [" + member.permission().name() + "]";
                if (member.title() != null && !member.title().isEmpty()) row += "  " + member.title();
                context.drawTextWithShadow(ProjectSettingsScreen.this.textRenderer,
                        Text.literal(row), getX(), getY() + 8, 0xFFFFFF);

                boolean canRemove = canAdmin && member.permission() != Permission.OWNER;
                if (canRemove) {
                    int delX = getX() + getRowWidth() - 60;
                    context.fill(delX, getY() + 4, delX + 56, getY() + 22, hovered ? 0x44FF4444 : 0x22FF4444);
                    context.drawTextWithShadow(ProjectSettingsScreen.this.textRenderer,
                            Text.literal("[Remove]"), delX + 2, getY() + 8, 0xFF4444);
                }
            }

            @Override
            public Text getNarration() {
                return Text.literal(member.username() + " " + member.permission().name());
            }
        }
    }

    // --- Status list widget ---

    private class StatusListWidget extends AlwaysSelectedEntryListWidget<StatusListWidget.StatusEntry> {

        public StatusListWidget(MinecraftClient client, int width, int height, int y,
                                int itemHeight, List<Status> statuses, boolean canEdit) {
            super(client, width, height, y, itemHeight);
            for (int i = 0; i < statuses.size(); i++) {
                addEntry(new StatusEntry(statuses.get(i), i, canEdit));
            }
        }

        @Override
        public int getRowWidth() {
            return ProjectSettingsScreen.this.width - 20;
        }

        @Override
        protected int getScrollbarX() {
            return ProjectSettingsScreen.this.width / 2 + getRowWidth() / 2 + 4;
        }

        public StatusEntry getEntryAt(double x, double y) {
            return getEntryAtPosition(x, y);
        }

        public class StatusEntry extends AlwaysSelectedEntryListWidget.Entry<StatusEntry> {
            final Status status;
            final int index;
            final boolean canEdit;

            public StatusEntry(Status status, int index, boolean canEdit) {
                this.status = status;
                this.index = index;
                this.canEdit = canEdit;
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawTextWithShadow(ProjectSettingsScreen.this.textRenderer,
                        Text.literal(status.key() + "  " + status.displayName() + "  " + status.textColor()),
                        getX(), getY() + 6, 0xFFFFFF);
                if (canEdit) {
                    int delX = getX() + getRowWidth() - 60;
                    context.fill(delX, getY() + 2, delX + 56, getY() + 20, hovered ? 0x44FF4444 : 0x22FF4444);
                    context.drawTextWithShadow(ProjectSettingsScreen.this.textRenderer,
                            Text.literal("[Remove]"), delX + 2, getY() + 6, 0xFF4444);
                }
            }

            @Override
            public Text getNarration() {
                return Text.literal(status.displayName());
            }
        }
    }

    // --- Ticket type list widget ---

    private class TypeListWidget extends AlwaysSelectedEntryListWidget<TypeListWidget.TypeEntry> {

        public TypeListWidget(MinecraftClient client, int width, int height, int y,
                              int itemHeight, List<TicketType> types, boolean canEdit) {
            super(client, width, height, y, itemHeight);
            for (int i = 0; i < types.size(); i++) {
                addEntry(new TypeEntry(types.get(i), i, canEdit));
            }
        }

        @Override
        public int getRowWidth() {
            return ProjectSettingsScreen.this.width - 20;
        }

        @Override
        protected int getScrollbarX() {
            return ProjectSettingsScreen.this.width / 2 + getRowWidth() / 2 + 4;
        }

        public TypeEntry getEntryAt(double x, double y) {
            return getEntryAtPosition(x, y);
        }

        public class TypeEntry extends AlwaysSelectedEntryListWidget.Entry<TypeEntry> {
            final TicketType type;
            final int index;
            final boolean canEdit;

            public TypeEntry(TicketType type, int index, boolean canEdit) {
                this.type = type;
                this.index = index;
                this.canEdit = canEdit;
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawTextWithShadow(ProjectSettingsScreen.this.textRenderer,
                        Text.literal(type.key() + "  " + type.displayName() + "  " + type.textColor()),
                        getX(), getY() + 6, 0xFFFFFF);
                if (canEdit) {
                    int delX = getX() + getRowWidth() - 60;
                    context.fill(delX, getY() + 2, delX + 56, getY() + 20, hovered ? 0x44FF4444 : 0x22FF4444);
                    context.drawTextWithShadow(ProjectSettingsScreen.this.textRenderer,
                            Text.literal("[Remove]"), delX + 2, getY() + 6, 0xFF4444);
                }
            }

            @Override
            public Text getNarration() {
                return Text.literal(type.displayName());
            }
        }
    }
}
