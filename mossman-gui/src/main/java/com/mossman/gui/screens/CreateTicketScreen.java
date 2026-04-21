package com.mossman.gui.screens;

import com.mossman.MossManApi;
import com.mossman.domain.entities.Priority;
import com.mossman.domain.entities.Project;
import com.mossman.domain.entities.Ticket;
import com.mossman.domain.entities.TicketType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.UUID;

public class CreateTicketScreen extends Screen {

    private static final int FIELD_WIDTH = 200;
    private static final int FIELD_HEIGHT = 20;
    private static final int ROW_HEIGHT = 30;
    private static final int FORM_START_Y = 45;

    private final Screen parent;
    private final Project project;

    private TextFieldWidget titleField;
    private TextFieldWidget descField;
    private TicketType selectedType;
    private Priority selectedPriority = Priority.MEDIUM;
    private String errorMessage;

    public CreateTicketScreen(Screen parent, Project project) {
        super(Text.literal("New Ticket"));
        this.parent = parent;
        this.project = project;
    }

    @Override
    protected void init() {
        errorMessage = null;
        int centerX = width / 2;
        int fieldX = centerX - FIELD_WIDTH / 2;
        int y = FORM_START_Y;

        titleField = new TextFieldWidget(textRenderer, fieldX, y, FIELD_WIDTH, FIELD_HEIGHT, Text.empty());
        titleField.setMaxLength(128);
        titleField.setPlaceholder(Text.literal("Ticket title (required)"));
        titleField.setChangedListener(t -> errorMessage = null);
        addDrawableChild(titleField);
        y += ROW_HEIGHT;

        List<TicketType> types = project.getTicketTypes();
        if (!types.isEmpty()) {
            if (selectedType == null) selectedType = types.get(0);
            @SuppressWarnings("unchecked")
            TicketType[] typeArr = types.toArray(new TicketType[0]);
            addDrawableChild(CyclingButtonWidget.<TicketType>builder(t -> Text.literal(t.displayName()), selectedType)
                    .values(typeArr)
                    .build(fieldX, y, FIELD_WIDTH, FIELD_HEIGHT, Text.literal("Type"),
                            (btn, val) -> selectedType = val));
        } else {
            selectedType = null;
        }
        y += ROW_HEIGHT;

        addDrawableChild(CyclingButtonWidget.<Priority>builder(p -> Text.literal(p.name()), selectedPriority)
                .values(Priority.values())
                .build(fieldX, y, FIELD_WIDTH, FIELD_HEIGHT, Text.literal("Priority"),
                        (btn, val) -> selectedPriority = val));
        y += ROW_HEIGHT;

        descField = new TextFieldWidget(textRenderer, fieldX, y, FIELD_WIDTH, FIELD_HEIGHT, Text.empty());
        descField.setMaxLength(512);
        descField.setPlaceholder(Text.literal("Description (optional)"));
        descField.setChangedListener(t -> errorMessage = null);
        addDrawableChild(descField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Create"), btn -> tryCreate())
                .dimensions(centerX - 104, height - 28, 100, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), btn -> close())
                .dimensions(centerX + 4, height - 28, 100, 20)
                .build());

        setInitialFocus(titleField);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);

        int fieldX = width / 2 - FIELD_WIDTH / 2;
        int y = FORM_START_Y;
        context.drawTextWithShadow(textRenderer, Text.literal("Title *"), fieldX, y - 10, 0xAAAAAA);
        y += ROW_HEIGHT;
        context.drawTextWithShadow(textRenderer, Text.literal("Type"), fieldX, y - 10, 0xAAAAAA);
        y += ROW_HEIGHT;
        context.drawTextWithShadow(textRenderer, Text.literal("Priority"), fieldX, y - 10, 0xAAAAAA);
        y += ROW_HEIGHT;
        context.drawTextWithShadow(textRenderer, Text.literal("Description"), fieldX, y - 10, 0xAAAAAA);

        if (errorMessage != null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(errorMessage),
                    width / 2, FORM_START_Y + 4 * ROW_HEIGHT + 4, 0xFF4444);
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private void tryCreate() {
        if (client.player == null) return;
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            errorMessage = "Title is required";
            return;
        }
        if (project.getTicketTypes().isEmpty() && selectedType == null) {
            errorMessage = "Project has no ticket types configured";
            return;
        }
        try {
            UUID playerId = client.player.getUuid();
            int nextNumber = MossManApi.getTicketRepository().getNextTicketNumber(project.getId());
            long now = System.currentTimeMillis();
            String typeKey = selectedType != null ? selectedType.key() : "";
            String defaultStatus = project.getStatuses().isEmpty() ? "OPEN" : project.getStatuses().get(0).key();
            Ticket ticket = new Ticket(
                    0, project.getId(), nextNumber,
                    title, descField.getText().trim(),
                    typeKey, defaultStatus, selectedPriority,
                    List.of(), List.of(), playerId, List.of(),
                    now, now, null
            );
            MossManApi.getCreateTicketUseCase().execute(ticket, playerId);
            client.setScreen(parent);
        } catch (Exception e) {
            errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }
    }
}
