package com.mossman.gui.screens;

import com.mossman.MossManApi;
import com.mossman.domain.entities.Comment;
import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Priority;
import com.mossman.domain.entities.Project;
import com.mossman.domain.entities.Status;
import com.mossman.domain.entities.Ticket;
import com.mossman.domain.entities.TicketType;
import com.mossman.domain.entities.TimeLog;
import com.mossman.domain.util.DurationParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TicketDetailScreen extends Screen {

    private static final int HEADER_HEIGHT = 44;
    private static final int TAB_BAR_HEIGHT = 24;
    private static final int FOOTER_HEIGHT = 36;
    private static final int CONTENT_TOP = HEADER_HEIGHT + TAB_BAR_HEIGHT;

    private final Screen parent;
    private final Project project;
    private Ticket ticket;
    private Permission playerPermission = Permission.VIEWER;

    // Tab state
    private int currentTab = 0;

    // Pending detail edits (persisted across tab changes)
    private String pendingStatus;
    private String pendingType;
    private Priority pendingPriority;
    private String pendingDescription;
    private boolean pendingInitialized = false;

    // Comment input
    private String commentDraft = "";

    // Time log input
    private String durationDraft = "";
    private String timeNoteDraft = "";

    private String errorMessage;

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("MM/dd HH:mm");

    public TicketDetailScreen(Screen parent, Project project, Ticket ticket) {
        super(Text.literal(ticket.getUserFriendlyKey(project.getTicketPrefix()) + "  " + ticket.getTitle()));
        this.parent = parent;
        this.project = project;
        this.ticket = ticket;
    }

    @Override
    protected void init() {
        errorMessage = null;

        UUID playerId = client.player != null ? client.player.getUuid() : null;
        if (playerId != null) {
            Optional<Member> member = project.getMembers().stream()
                    .filter(m -> m.uuid().equals(playerId))
                    .findFirst();
            playerPermission = member.map(Member::permission)
                    .orElse(project.getExternalUserPermission());
        }

        // Initialize pending values once (preserve across tab changes)
        if (!pendingInitialized) {
            pendingStatus = ticket.getStatus();
            pendingType = ticket.getType();
            pendingPriority = ticket.getPriority();
            pendingDescription = ticket.getDescription() != null ? ticket.getDescription() : "";
            pendingInitialized = true;
        }

        // Tab buttons
        addDrawableChild(ButtonWidget.builder(Text.literal("Details"), btn -> switchTab(0))
                .dimensions(width / 2 - 152, HEADER_HEIGHT + 2, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Comments"), btn -> switchTab(1))
                .dimensions(width / 2 - 50, HEADER_HEIGHT + 2, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Time Logs"), btn -> switchTab(2))
                .dimensions(width / 2 + 52, HEADER_HEIGHT + 2, 100, 20).build());

        // Build current tab content
        switch (currentTab) {
            case 0 -> initDetailsTab(playerId);
            case 1 -> initCommentsTab(playerId);
            case 2 -> initTimeLogsTab(playerId);
        }

        // Footer
        int footerY = height - 28;
        if (currentTab == 0 && playerPermission.ordinal() >= Permission.EDITOR.ordinal()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Save"), btn -> trySave(playerId))
                    .dimensions(width / 2 - 104, footerY, 100, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> close())
                    .dimensions(width / 2 + 4, footerY, 100, 20).build());
        } else {
            addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> close())
                    .dimensions(width / 2 - 50, footerY, 100, 20).build());
        }
    }

    private void initDetailsTab(UUID playerId) {
        boolean canEdit = playerPermission.ordinal() >= Permission.EDITOR.ordinal();
        int fieldW = 180;
        int fieldX = width / 2 - fieldW / 2;
        int y = CONTENT_TOP + 8;

        if (canEdit && !project.getStatuses().isEmpty()) {
            Status[] statuses = project.getStatuses().toArray(new Status[0]);
            Status curStatus = project.getStatuses().stream()
                    .filter(s -> s.key().equals(pendingStatus))
                    .findFirst()
                    .orElse(statuses[0]);
            addDrawableChild(CyclingButtonWidget.<Status>builder(s -> Text.literal(s.displayName()), curStatus)
                    .values(statuses)
                    .build(fieldX, y, fieldW, 20, Text.literal("Status"),
                            (btn, val) -> pendingStatus = val.key()));
        }
        y += 28;

        if (canEdit && !project.getTicketTypes().isEmpty()) {
            TicketType[] types = project.getTicketTypes().toArray(new TicketType[0]);
            TicketType curType = project.getTicketTypes().stream()
                    .filter(t -> t.key().equals(pendingType))
                    .findFirst()
                    .orElse(types[0]);
            addDrawableChild(CyclingButtonWidget.<TicketType>builder(t -> Text.literal(t.displayName()), curType)
                    .values(types)
                    .build(fieldX, y, fieldW, 20, Text.literal("Type"),
                            (btn, val) -> pendingType = val.key()));
        }
        y += 28;

        if (canEdit) {
            addDrawableChild(CyclingButtonWidget.<Priority>builder(p -> Text.literal(p.name()), pendingPriority)
                    .values(Priority.values())
                    .build(fieldX, y, fieldW, 20, Text.literal("Priority"),
                            (btn, val) -> pendingPriority = val));
        }
        y += 28;

        if (canEdit) {
            TextFieldWidget descField = new TextFieldWidget(textRenderer, fieldX, y, fieldW, 20, Text.empty());
            descField.setMaxLength(512);
            descField.setText(pendingDescription);
            descField.setChangedListener(t -> pendingDescription = t);
            addDrawableChild(descField);
        }
        y += 28;

        // Watch/Unwatch button (VIEWER+)
        if (playerId != null && playerPermission.ordinal() >= Permission.VIEWER.ordinal()) {
            boolean watching = ticket.getObservers().contains(playerId);
            addDrawableChild(ButtonWidget.builder(
                    Text.literal(watching ? "Unwatch" : "Watch"),
                    btn -> toggleWatch(playerId, watching))
                    .dimensions(fieldX, y, 86, 20).build());
        }

        // Assign/Unassign (EDITOR+)
        if (playerId != null && playerPermission.ordinal() >= Permission.EDITOR.ordinal()) {
            boolean assigned = ticket.getAssignees().contains(playerId);
            addDrawableChild(ButtonWidget.builder(
                    Text.literal(assigned ? "Unassign me" : "Assign me"),
                    btn -> toggleAssign(playerId, assigned))
                    .dimensions(fieldX + 90, y, 90, 20).build());
        }
    }

    private void initCommentsTab(UUID playerId) {
        List<Comment> comments;
        try {
            comments = MossManApi.getCommentRepository().findByTicketId(ticket.getId());
        } catch (Exception e) {
            comments = List.of();
            errorMessage = "Failed to load comments: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }

        boolean canComment = playerId != null && playerPermission.ordinal() >= Permission.CREATOR.ordinal();
        int listBottom = canComment ? height - FOOTER_HEIGHT - 28 : height - FOOTER_HEIGHT;
        commentListWidget = new CommentListWidget(
                client, width, listBottom - CONTENT_TOP, CONTENT_TOP, 36, comments, playerId);
        addDrawableChild(commentListWidget);

        if (canComment) {
            int inputY = listBottom + 4;
            TextFieldWidget msgField = new TextFieldWidget(textRenderer, 10, inputY, width - 120, 20, Text.empty());
            msgField.setMaxLength(1024);
            msgField.setPlaceholder(Text.literal("Add a comment..."));
            msgField.setText(commentDraft);
            msgField.setChangedListener(t -> commentDraft = t);
            addDrawableChild(msgField);

            addDrawableChild(ButtonWidget.builder(Text.literal("Post"), btn -> {
                String msg = commentDraft.trim();
                if (!msg.isEmpty() && playerId != null && client.player != null) {
                    try {
                        MossManApi.getAddCommentUseCase().execute(
                                ticket.getId(), playerId, client.player.getGameProfile().name(), msg);
                        commentDraft = "";
                        switchTab(1);
                    } catch (Exception e) {
                        errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    }
                }
            }).dimensions(width - 106, inputY, 96, 20).build());
        }
    }

    private void initTimeLogsTab(UUID playerId) {
        long totalMinutes = 0;
        List<TimeLog> logs;
        try {
            totalMinutes = MossManApi.getTimeLogRepository().sumMinutesByTicketId(ticket.getId());
            logs = MossManApi.getTimeLogRepository().findByTicketId(ticket.getId(), 0, 200);
        } catch (Exception e) {
            logs = List.of();
            errorMessage = "Failed to load time logs: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }

        boolean canLog = playerId != null && playerPermission.ordinal() >= Permission.CREATOR.ordinal();
        int listBottom = canLog ? height - FOOTER_HEIGHT - 28 : height - FOOTER_HEIGHT;

        // Total time rendered in render()
        timeLogListWidget = new TimeLogListWidget(
                client, width, listBottom - CONTENT_TOP - 18, CONTENT_TOP + 18, 24, logs, playerId);
        addDrawableChild(timeLogListWidget);

        if (canLog) {
            int inputY = listBottom + 4;
            TextFieldWidget durField = new TextFieldWidget(textRenderer, 10, inputY, 100, 20, Text.empty());
            durField.setMaxLength(32);
            durField.setPlaceholder(Text.literal("e.g. 1h 30m"));
            durField.setText(durationDraft);
            durField.setChangedListener(t -> durationDraft = t);
            addDrawableChild(durField);

            TextFieldWidget noteField = new TextFieldWidget(textRenderer, 114, inputY, width - 220, 20, Text.empty());
            noteField.setMaxLength(256);
            noteField.setPlaceholder(Text.literal("Note (optional)"));
            noteField.setText(timeNoteDraft);
            noteField.setChangedListener(t -> timeNoteDraft = t);
            addDrawableChild(noteField);

            addDrawableChild(ButtonWidget.builder(Text.literal("Log"), btn -> {
                String dur = durationDraft.trim();
                if (!dur.isEmpty() && playerId != null && client.player != null) {
                    try {
                        MossManApi.getLogTimeUseCase().execute(
                                ticket.getId(), playerId, client.player.getGameProfile().name(),
                                dur, timeNoteDraft.trim());
                        durationDraft = "";
                        timeNoteDraft = "";
                        switchTab(2);
                    } catch (Exception e) {
                        errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    }
                }
            }).dimensions(width - 102, inputY, 92, 20).build());
        }
    }

    private void switchTab(int tab) {
        currentTab = tab;
        this.init(this.width, this.height);
    }

    private void toggleWatch(UUID playerId, boolean currentlyWatching) {
        try {
            if (currentlyWatching) {
                ticket = MossManApi.getUnobserveTicketUseCase().execute(ticket.getId(), playerId);
            } else {
                ticket = MossManApi.getObserveTicketUseCase().execute(ticket.getId(), playerId);
            }
            switchTab(currentTab);
        } catch (Exception e) {
            errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }
    }

    private void toggleAssign(UUID playerId, boolean currentlyAssigned) {
        try {
            if (currentlyAssigned) {
                ticket = MossManApi.getUnassignTicketUseCase().execute(ticket.getId(), playerId, playerId);
            } else {
                ticket = MossManApi.getAssignTicketUseCase().execute(ticket.getId(), playerId, playerId);
            }
            switchTab(currentTab);
        } catch (Exception e) {
            errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }
    }

    private void trySave(UUID playerId) {
        if (playerId == null) return;
        try {
            Map<String, String> patch = new HashMap<>();
            patch.put("status", pendingStatus);
            patch.put("type", pendingType);
            patch.put("priority", pendingPriority.name());
            patch.put("description", pendingDescription);
            ticket = MossManApi.getUpdateTicketUseCase().execute(ticket.getId(), playerId, patch);
            pendingInitialized = false; // reload from saved ticket
            switchTab(currentTab);
        } catch (Exception e) {
            errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Header: ticket key + title
        String key = ticket.getUserFriendlyKey(project.getTicketPrefix());
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(key + "  " + ticket.getTitle()),
                width / 2, 8, 0xFFFFFF);
        String badges = ticket.getStatus() + "  " + ticket.getType() + "  " + ticket.getPriority().name();
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(badges), width / 2, 22, 0xAAAAAA);

        // Tab underline
        int tabLeft = width / 2 - 152;
        int tabW = 100;
        int underlineY = HEADER_HEIGHT + TAB_BAR_HEIGHT - 2;
        context.fill(tabLeft + currentTab * 104, underlineY, tabLeft + currentTab * 104 + tabW, underlineY + 2, 0xFFFFFFFF);

        // For time logs tab, show total
        if (currentTab == 2) {
            try {
                long total = MossManApi.getTimeLogRepository().sumMinutesByTicketId(ticket.getId());
                context.drawTextWithShadow(textRenderer,
                        Text.literal("Total: " + DurationParser.format(total)), 10, CONTENT_TOP + 4, 0xFFFFFF);
            } catch (Exception ignored) {}
        }

        // Error message
        if (errorMessage != null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(errorMessage),
                    width / 2, height - FOOTER_HEIGHT - 12, 0xFF4444);
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private CommentListWidget commentListWidget;
    private TimeLogListWidget timeLogListWidget;

    @Override
    public boolean mouseClicked(Click click, boolean active) {
        if (click.button() == 0) {
            double mouseX = click.x();
            double mouseY = click.y();
            if (currentTab == 1 && commentListWidget != null) {
                CommentListWidget.CommentEntry entry = commentListWidget.getEntryAt(mouseX, mouseY);
                if (entry != null && entry.playerId != null) {
                    boolean canDelete = entry.playerId.equals(entry.comment.authorId())
                            || playerPermission.ordinal() >= Permission.EDITOR.ordinal();
                    if (canDelete) {
                        int delX = entry.getX() + commentListWidget.getRowWidth() - 50;
                        if (mouseX >= delX && mouseX <= delX + 46
                                && mouseY >= entry.getY() + 2 && mouseY <= entry.getY() + 14) {
                            try {
                                MossManApi.getDeleteCommentUseCase().execute(entry.comment.id(), entry.playerId);
                                switchTab(1);
                            } catch (Exception e) {
                                errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                            }
                            return true;
                        }
                    }
                }
            } else if (currentTab == 2 && timeLogListWidget != null) {
                TimeLogListWidget.TimeLogEntry entry = timeLogListWidget.getEntryAt(mouseX, mouseY);
                if (entry != null && entry.playerId != null) {
                    boolean canDelete = entry.playerId.equals(entry.log.workerId())
                            || playerPermission.ordinal() >= Permission.EDITOR.ordinal();
                    if (canDelete) {
                        int delX = entry.getX() + timeLogListWidget.getRowWidth() - 50;
                        if (mouseX >= delX && mouseX <= delX + 46
                                && mouseY >= entry.getY() + 4 && mouseY <= entry.getY() + 16) {
                            try {
                                MossManApi.getDeleteTimeLogUseCase().execute(entry.log.id(), entry.playerId);
                                switchTab(2);
                            } catch (Exception e) {
                                errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(click, active);
    }

    // --- Comment list widget ---

    private class CommentListWidget extends AlwaysSelectedEntryListWidget<CommentListWidget.CommentEntry> {

        public CommentListWidget(MinecraftClient client, int width, int height, int y,
                                 int itemHeight, List<Comment> comments, UUID playerId) {
            super(client, width, height, y, itemHeight);
            for (Comment c : comments) {
                addEntry(new CommentEntry(c, playerId));
            }
        }

        public CommentEntry getEntryAt(double x, double y) {
            return getEntryAtPosition(x, y);
        }

        @Override
        public int getRowWidth() {
            return TicketDetailScreen.this.width - 20;
        }

        @Override
        protected int getScrollbarX() {
            return TicketDetailScreen.this.width / 2 + getRowWidth() / 2 + 4;
        }

        public class CommentEntry extends AlwaysSelectedEntryListWidget.Entry<CommentEntry> {
            final Comment comment;
            final UUID playerId;

            public CommentEntry(Comment comment, UUID playerId) {
                this.comment = comment;
                this.playerId = playerId;
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                String header = comment.authorName() + "  " + DATE_FMT.format(new Date(comment.createdAt()));
                context.drawTextWithShadow(TicketDetailScreen.this.textRenderer,
                        Text.literal(header), getX(), getY() + 2, 0xAAAAAA);
                context.drawTextWithShadow(TicketDetailScreen.this.textRenderer,
                        Text.literal(comment.message()), getX(), getY() + 14, 0xFFFFFF);

                boolean canDelete = (playerId != null && playerId.equals(comment.authorId()))
                        || playerPermission.ordinal() >= Permission.EDITOR.ordinal();
                if (canDelete) {
                    int delX = getX() + getRowWidth() - 50;
                    context.fill(delX, getY() + 2, delX + 46, getY() + 14, hovered ? 0x44FF4444 : 0x22FF4444);
                    context.drawTextWithShadow(TicketDetailScreen.this.textRenderer,
                            Text.literal("[Delete]"), delX + 2, getY() + 2, 0xFF4444);
                }
            }

            @Override
            public Text getNarration() {
                return Text.literal(comment.authorName() + ": " + comment.message());
            }
        }
    }

    // --- Time log list widget ---

    private class TimeLogListWidget extends AlwaysSelectedEntryListWidget<TimeLogListWidget.TimeLogEntry> {

        public TimeLogListWidget(MinecraftClient client, int width, int height, int y,
                                 int itemHeight, List<TimeLog> logs, UUID playerId) {
            super(client, width, height, y, itemHeight);
            for (TimeLog log : logs) {
                addEntry(new TimeLogEntry(log, playerId));
            }
        }

        @Override
        public int getRowWidth() {
            return TicketDetailScreen.this.width - 20;
        }

        @Override
        protected int getScrollbarX() {
            return TicketDetailScreen.this.width / 2 + getRowWidth() / 2 + 4;
        }

        public TimeLogEntry getEntryAt(double x, double y) {
            return getEntryAtPosition(x, y);
        }

        public class TimeLogEntry extends AlwaysSelectedEntryListWidget.Entry<TimeLogEntry> {
            final TimeLog log;
            final UUID playerId;

            public TimeLogEntry(TimeLog log, UUID playerId) {
                this.log = log;
                this.playerId = playerId;
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                String date = new SimpleDateFormat("MM/dd").format(new Date(log.loggedAt()));
                String duration = DurationParser.format(log.minutes());
                String row = log.workerName() + "  " + date + "  " + duration;
                if (log.note() != null && !log.note().isEmpty()) row += "  \u2014 " + log.note();
                context.drawTextWithShadow(TicketDetailScreen.this.textRenderer,
                        Text.literal(row), getX(), getY() + 7, 0xFFFFFF);

                boolean canDelete = (playerId != null && playerId.equals(log.workerId()))
                        || playerPermission.ordinal() >= Permission.EDITOR.ordinal();
                if (canDelete) {
                    int delX = getX() + getRowWidth() - 50;
                    context.fill(delX, getY() + 4, delX + 46, getY() + 16, hovered ? 0x44FF4444 : 0x22FF4444);
                    context.drawTextWithShadow(TicketDetailScreen.this.textRenderer,
                            Text.literal("[Delete]"), delX + 2, getY() + 4, 0xFF4444);
                }
            }

            @Override
            public Text getNarration() {
                return Text.literal(log.workerName() + " " + DurationParser.format(log.minutes()));
            }
        }
    }
}
