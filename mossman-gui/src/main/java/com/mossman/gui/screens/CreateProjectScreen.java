package com.mossman.gui.screens;

import com.mossman.MossManApi;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Project;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class CreateProjectScreen extends Screen {

    private static final int FIELD_WIDTH = 200;
    private static final int FIELD_HEIGHT = 20;
    private static final int ROW_HEIGHT = 30;
    private static final int FORM_START_Y = 45;

    private final Screen parent;

    private TextFieldWidget prefixField;
    private TextFieldWidget nameField;
    private TextFieldWidget descField;
    private TextFieldWidget iconField;
    private Permission selectedPermission = Permission.FORBID;
    private String errorMessage;

    public CreateProjectScreen(Screen parent) {
        super(Text.literal("Create Project"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        errorMessage = null;
        int centerX = width / 2;
        int fieldX = centerX - FIELD_WIDTH / 2;
        int y = FORM_START_Y;

        prefixField = new TextFieldWidget(textRenderer, fieldX, y, FIELD_WIDTH, FIELD_HEIGHT, Text.empty());
        prefixField.setMaxLength(8);
        prefixField.setPlaceholder(Text.literal("e.g. PROJ"));
        prefixField.setChangedListener(t -> errorMessage = null);
        addDrawableChild(prefixField);
        y += ROW_HEIGHT;

        nameField = new TextFieldWidget(textRenderer, fieldX, y, FIELD_WIDTH, FIELD_HEIGHT, Text.empty());
        nameField.setMaxLength(64);
        nameField.setPlaceholder(Text.literal("My Project"));
        nameField.setChangedListener(t -> errorMessage = null);
        addDrawableChild(nameField);
        y += ROW_HEIGHT;

        descField = new TextFieldWidget(textRenderer, fieldX, y, FIELD_WIDTH, FIELD_HEIGHT, Text.empty());
        descField.setMaxLength(256);
        descField.setPlaceholder(Text.literal("(optional)"));
        descField.setChangedListener(t -> errorMessage = null);
        addDrawableChild(descField);
        y += ROW_HEIGHT;

        iconField = new TextFieldWidget(textRenderer, fieldX, y, FIELD_WIDTH, FIELD_HEIGHT, Text.empty());
        iconField.setMaxLength(128);
        iconField.setPlaceholder(Text.literal("namespace:path  (optional)"));
        iconField.setChangedListener(t -> errorMessage = null);
        addDrawableChild(iconField);
        y += ROW_HEIGHT;

        addDrawableChild(CyclingButtonWidget.<Permission>builder(p -> Text.literal(p.name()), Permission.FORBID)
                .values(Permission.FORBID, Permission.VIEWER, Permission.CREATOR, Permission.EDITOR, Permission.ADMIN)
                .build(fieldX, y, FIELD_WIDTH, FIELD_HEIGHT, Text.literal("External Access"),
                        (btn, val) -> selectedPermission = val));

        addDrawableChild(ButtonWidget.builder(Text.literal("Create"), btn -> tryCreate())
                .dimensions(centerX - 104, height - 28, 100, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), btn -> close())
                .dimensions(centerX + 4, height - 28, 100, 20)
                .build());

        setInitialFocus(prefixField);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);

        int fieldX = width / 2 - FIELD_WIDTH / 2;
        int y = FORM_START_Y;
        context.drawTextWithShadow(textRenderer, Text.literal("Ticket Prefix *"), fieldX, y - 10, 0xAAAAAA);
        y += ROW_HEIGHT;
        context.drawTextWithShadow(textRenderer, Text.literal("Name *"), fieldX, y - 10, 0xAAAAAA);
        y += ROW_HEIGHT;
        context.drawTextWithShadow(textRenderer, Text.literal("Description"), fieldX, y - 10, 0xAAAAAA);
        y += ROW_HEIGHT;
        context.drawTextWithShadow(textRenderer, Text.literal("Icon Texture"), fieldX, y - 10, 0xAAAAAA);
        y += ROW_HEIGHT;
        context.drawTextWithShadow(textRenderer, Text.literal("External Access"), fieldX, y - 10, 0xAAAAAA);

        if (errorMessage != null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(errorMessage),
                    width / 2, FORM_START_Y + 5 * ROW_HEIGHT + 4, 0xFF4444);
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private void tryCreate() {
        if (client.player == null) return;
        try {
            MossManApi.getCreateProjectUseCase().execute(
                    Project.builder()
                            .ticketPrefix(prefixField.getText().toUpperCase().trim())
                            .name(nameField.getText().trim())
                            .description(descField.getText().trim())
                            .iconTexture(iconField.getText().trim())
                            .externalUserPermission(selectedPermission),
                    client.player.getUuid(),
                    client.player.getGameProfile().name()
            );
            client.setScreen(parent);
        } catch (IllegalArgumentException e) {
            errorMessage = e.getMessage();
        }
    }
}
