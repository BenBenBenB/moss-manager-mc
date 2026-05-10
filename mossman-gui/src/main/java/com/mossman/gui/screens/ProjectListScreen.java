package com.mossman.gui.screens;

import com.mossman.MossManApi;
import com.mossman.domain.entities.Project;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.UUID;

public class ProjectListScreen extends Screen {

    private static final int HEADER_HEIGHT = 32;
    private static final int FOOTER_HEIGHT = 36;
    private static final int ITEM_HEIGHT = 24;

    private final Screen parent;
    private ProjectListWidget projectList;

    public ProjectListScreen(Screen parent) {
        super(Text.literal("Projects"));
        this.parent = parent;
    }

    private String loadError;

    @Override
    protected void init() {
        loadError = null;
        UUID playerId = client.player != null ? client.player.getUuid() : null;

        List<Project> projects;
        try {
            projects = playerId != null
                    ? MossManApi.getProjectRepository().findAllForUser(playerId, 0, 1000)
                    : MossManApi.getProjectRepository().findAll(0, 1000);
        } catch (Exception e) {
            projects = List.of();
            loadError = "Failed to load projects: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }

        long unread = 0;
        if (playerId != null) {
            try {
                unread = MossManApi.getMailRepository().countUnread(playerId);
            } catch (Exception ignored) {}
        }

        projectList = new ProjectListWidget(client, width, height - HEADER_HEIGHT - FOOTER_HEIGHT, HEADER_HEIGHT, ITEM_HEIGHT, projects);
        addDrawableChild(projectList);

        String inboxLabel = unread > 0 ? "Inbox (" + unread + ")" : "Inbox";
        addDrawableChild(ButtonWidget.builder(Text.literal("New Project"), btn -> openCreateScreen())
                .dimensions(width / 2 - 154, height - 28, 100, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal(inboxLabel), btn -> client.setScreen(new MailInboxScreen(this)))
                .dimensions(width / 2 - 50, height - 28, 100, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> close())
                .dimensions(width / 2 + 54, height - 28, 100, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);
        if (loadError != null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(loadError), width / 2, HEADER_HEIGHT + 4, 0xFF4444);
        }
    }

    Screen getParent() { return parent; }

    private void openCreateScreen() {
        client.setScreen(new CreateProjectScreen(this));
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private class ProjectListWidget extends AlwaysSelectedEntryListWidget<ProjectListWidget.ProjectEntry> {

        public ProjectListWidget(MinecraftClient client, int width, int height, int y, int itemHeight, List<Project> projects) {
            super(client, width, height, y, itemHeight);
            for (Project project : projects) {
                addEntry(new ProjectEntry(project));
            }
        }

        @Override
        public int getRowWidth() {
            return ProjectListScreen.this.width - 20;
        }

        @Override
        protected int getScrollbarX() {
            return ProjectListScreen.this.width / 2 + getRowWidth() / 2 + 4;
        }

        public class ProjectEntry extends AlwaysSelectedEntryListWidget.Entry<ProjectEntry> {

            private final Project project;

            public ProjectEntry(Project project) {
                this.project = project;
            }

            @Override
            public boolean mouseClicked(Click click, boolean propagate) {
                ProjectListWidget.this.setSelected(this);
                ProjectListScreen.this.client.setScreen(new ProjectDetailScreen(ProjectListScreen.this, this.project));
                return true;
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawTextWithShadow(
                        ProjectListScreen.this.textRenderer,
                        Text.literal("[" + project.getTicketPrefix() + "] " + project.getName()),
                        getX(), getY() + 2, 0xFFFFFF);
                String desc = project.getDescription();
                if (desc != null && !desc.isEmpty()) {
                    context.drawTextWithShadow(
                            ProjectListScreen.this.textRenderer,
                            Text.literal(desc),
                            getX(), getY() + 13, 0xAAAAAA);
                }
            }

            @Override
            public Text getNarration() {
                return Text.literal(project.getName());
            }
        }
    }
}
