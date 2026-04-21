package com.mossman.gui.screens;

import com.mossman.MossManApi;
import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Priority;
import com.mossman.domain.entities.Project;
import com.mossman.domain.entities.Status;
import com.mossman.domain.entities.Ticket;
import com.mossman.domain.query.TicketFilter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ProjectDetailScreen extends Screen {

    private static final int HEADER_HEIGHT = 50;
    private static final int FILTER_ROW_HEIGHT = 28;
    private static final int FOOTER_HEIGHT = 36;
    private static final int ITEM_HEIGHT = 24;

    private final Screen parent;
    private final Project project;

    private Permission playerPermission = Permission.VIEWER;
    private String statusFilter = null;
    private Priority priorityFilter = null;
    private String loadError;

    // Cycling status values: null = All, then each status key
    private List<String> statusCycle;

    public ProjectDetailScreen(Screen parent, Project project) {
        super(Text.literal("[" + project.getTicketPrefix() + "] " + project.getName()));
        this.parent = parent;
        this.project = project;

        // Build status cycle list (null = All)
        statusCycle = new ArrayList<>();
        statusCycle.add(null);
        project.getStatuses().forEach(s -> statusCycle.add(s.key()));
    }

    @Override
    protected void init() {
        loadError = null;

        UUID playerId = client.player != null ? client.player.getUuid() : null;
        if (playerId != null) {
            Optional<Member> member = project.getMembers().stream()
                    .filter(m -> m.uuid().equals(playerId))
                    .findFirst();
            playerPermission = member.map(Member::permission)
                    .orElse(project.getExternalUserPermission());
        }

        List<Ticket> tickets;
        try {
            Map<String, String> filterMap = new HashMap<>();
            if (statusFilter != null) filterMap.put("status", statusFilter);
            if (priorityFilter != null) filterMap.put("priority", priorityFilter.name());
            TicketFilter filter = TicketFilter.of(filterMap);
            tickets = MossManApi.getTicketRepository().findByProjectId(project.getId(), filter, 0, 500);
        } catch (Exception e) {
            tickets = List.of();
            loadError = "Failed to load tickets: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }

        int listTop = HEADER_HEIGHT + FILTER_ROW_HEIGHT;
        TicketListWidget ticketList = new TicketListWidget(
                client, width, height - listTop - FOOTER_HEIGHT, listTop, ITEM_HEIGHT, tickets);
        addDrawableChild(ticketList);

        // Filter row
        int filterY = HEADER_HEIGHT + 4;
        String statusLabel = "Status: " + (statusFilter == null ? "All" : getStatusDisplayName(statusFilter));
        addDrawableChild(ButtonWidget.builder(Text.literal(statusLabel), btn -> {
            int idx = statusCycle.indexOf(statusFilter);
            statusFilter = statusCycle.get((idx + 1) % statusCycle.size());
            this.init(this.width, this.height);
        }).dimensions(10, filterY, 110, 20).build());

        String priorityLabel = "Priority: " + (priorityFilter == null ? "All" : priorityFilter.name());
        addDrawableChild(ButtonWidget.builder(Text.literal(priorityLabel), btn -> {
            Priority[] vals = Priority.values();
            priorityFilter = priorityFilter == null ? vals[0] : (priorityFilter.ordinal() + 1 < vals.length ? vals[priorityFilter.ordinal() + 1] : null);
            this.init(this.width, this.height);
        }).dimensions(124, filterY, 110, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), btn -> {
            statusFilter = null;
            priorityFilter = null;
            this.init(this.width, this.height);
        }).dimensions(238, filterY, 50, 20).build());

        // Footer buttons
        int footerY = height - 28;
        List<ButtonWidget> footerBtns = new ArrayList<>();

        if (playerPermission.ordinal() >= Permission.CREATOR.ordinal()) {
            footerBtns.add(ButtonWidget.builder(Text.literal("New Ticket"),
                    btn -> client.setScreen(new CreateTicketScreen(this, project)))
                    .dimensions(0, footerY, 100, 20).build());
        }
        if (playerPermission.ordinal() >= Permission.EDITOR.ordinal()) {
            footerBtns.add(ButtonWidget.builder(Text.literal("Settings"),
                    btn -> client.setScreen(new ProjectSettingsScreen(this, project)))
                    .dimensions(0, footerY, 100, 20).build());
        }
        footerBtns.add(ButtonWidget.builder(Text.literal("Back"), btn -> close())
                .dimensions(0, footerY, 100, 20).build());

        // Layout footer buttons centered
        int totalW = footerBtns.size() * 100 + (footerBtns.size() - 1) * 4;
        int startX = width / 2 - totalW / 2;
        for (ButtonWidget btn : footerBtns) {
            btn.setX(startX);
            addDrawableChild(btn);
            startX += 104;
        }
    }

    private String getStatusDisplayName(String key) {
        return project.getStatuses().stream()
                .filter(s -> s.key().equals(key))
                .map(Status::displayName)
                .findFirst()
                .orElse(key);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);
        String desc = project.getDescription();
        if (desc != null && !desc.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(desc), width / 2, 24, 0xAAAAAA);
        }
        if (loadError != null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(loadError),
                    width / 2, HEADER_HEIGHT + FILTER_ROW_HEIGHT + 4, 0xFF4444);
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private class TicketListWidget extends AlwaysSelectedEntryListWidget<TicketListWidget.TicketEntry> {

        public TicketListWidget(MinecraftClient client, int width, int height, int y, int itemHeight, List<Ticket> tickets) {
            super(client, width, height, y, itemHeight);
            for (Ticket ticket : tickets) {
                addEntry(new TicketEntry(ticket));
            }
            initialized = true;
        }

        @Override
        public int getRowWidth() {
            return ProjectDetailScreen.this.width - 20;
        }

        @Override
        protected int getScrollbarX() {
            return ProjectDetailScreen.this.width / 2 + getRowWidth() / 2 + 4;
        }

        private boolean initialized = false;

        @Override
        public void setSelected(TicketEntry entry) {
            super.setSelected(entry);
            if (initialized && entry != null) {
                ProjectDetailScreen.this.client.setScreen(
                        new TicketDetailScreen(ProjectDetailScreen.this, project, entry.ticket));
            }
        }

        public class TicketEntry extends AlwaysSelectedEntryListWidget.Entry<TicketEntry> {

            private final Ticket ticket;

            public TicketEntry(Ticket ticket) {
                this.ticket = ticket;
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                String key = ticket.getUserFriendlyKey(project.getTicketPrefix());
                context.drawTextWithShadow(ProjectDetailScreen.this.textRenderer,
                        Text.literal(key + "  " + ticket.getTitle()), getX(), getY() + 2, 0xFFFFFF);
                String right = ticket.getStatus() + " \u00b7 " + ticket.getType() + " \u00b7 " + ticket.getPriority().name();
                int rightX = getX() + getRowWidth() - ProjectDetailScreen.this.textRenderer.getWidth(right) - 4;
                context.drawTextWithShadow(ProjectDetailScreen.this.textRenderer,
                        Text.literal(right), rightX, getY() + 2, 0xAAAAAA);
            }

            @Override
            public Text getNarration() {
                return Text.literal(ticket.getUserFriendlyKey(project.getTicketPrefix()) + " " + ticket.getTitle());
            }
        }
    }
}
