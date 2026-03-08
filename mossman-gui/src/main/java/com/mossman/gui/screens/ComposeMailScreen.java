package com.mossman.gui.screens;

import com.mossman.MossManApi;
import com.mossman.domain.entities.MailMessage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.Optional;
import java.util.UUID;

public class ComposeMailScreen extends Screen {

    private static final int FIELD_WIDTH = 200;
    private static final int FIELD_HEIGHT = 20;
    private static final int ROW_HEIGHT = 30;
    private static final int FORM_START_Y = 45;

    private final Screen parent;

    private TextFieldWidget toField;
    private TextFieldWidget subjectField;
    private TextFieldWidget bodyField;
    private String errorMessage;

    // Pre-fill values (set before init() via prefill())
    private String prefillTo = "";
    private String prefillSubject = "";
    private String prefillBody = "";

    public ComposeMailScreen(Screen parent) {
        super(Text.literal("Compose Mail"));
        this.parent = parent;
    }

    /** Pre-fill fields when replying. Call before setScreen(). */
    public void prefill(String to, String subject, String body) {
        this.prefillTo = to != null ? to : "";
        this.prefillSubject = subject != null ? subject : "";
        this.prefillBody = body != null ? body : "";
    }

    @Override
    protected void init() {
        errorMessage = null;
        int centerX = width / 2;
        int fieldX = centerX - FIELD_WIDTH / 2;
        int y = FORM_START_Y;

        toField = new TextFieldWidget(textRenderer, fieldX, y, FIELD_WIDTH, FIELD_HEIGHT, Text.empty());
        toField.setMaxLength(64);
        toField.setPlaceholder(Text.literal("Player name"));
        toField.setText(prefillTo);
        toField.setChangedListener(t -> errorMessage = null);
        addDrawableChild(toField);
        y += ROW_HEIGHT;

        subjectField = new TextFieldWidget(textRenderer, fieldX, y, FIELD_WIDTH, FIELD_HEIGHT, Text.empty());
        subjectField.setMaxLength(128);
        subjectField.setPlaceholder(Text.literal("Subject"));
        subjectField.setText(prefillSubject);
        subjectField.setChangedListener(t -> errorMessage = null);
        addDrawableChild(subjectField);
        y += ROW_HEIGHT;

        bodyField = new TextFieldWidget(textRenderer, fieldX, y, FIELD_WIDTH, FIELD_HEIGHT, Text.empty());
        bodyField.setMaxLength(1024);
        bodyField.setPlaceholder(Text.literal("Message body"));
        bodyField.setText(prefillBody);
        bodyField.setChangedListener(t -> errorMessage = null);
        addDrawableChild(bodyField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Send"), btn -> trySend())
                .dimensions(centerX - 104, height - 28, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), btn -> close())
                .dimensions(centerX + 4, height - 28, 100, 20).build());

        setInitialFocus(toField);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);

        int fieldX = width / 2 - FIELD_WIDTH / 2;
        int y = FORM_START_Y;
        context.drawTextWithShadow(textRenderer, Text.literal("To *"), fieldX, y - 10, 0xAAAAAA);
        y += ROW_HEIGHT;
        context.drawTextWithShadow(textRenderer, Text.literal("Subject *"), fieldX, y - 10, 0xAAAAAA);
        y += ROW_HEIGHT;
        context.drawTextWithShadow(textRenderer, Text.literal("Body"), fieldX, y - 10, 0xAAAAAA);

        if (errorMessage != null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(errorMessage),
                    width / 2, FORM_START_Y + 3 * ROW_HEIGHT + 4, 0xFF4444);
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private void trySend() {
        if (client.player == null) return;
        String toName = toField.getText().trim();
        String subject = subjectField.getText().trim();
        String body = bodyField.getText().trim();

        if (toName.isEmpty()) { errorMessage = "Recipient name is required"; return; }
        if (subject.isEmpty()) { errorMessage = "Subject is required"; return; }

        UUID recipientId = resolvePlayerUuid(toName);
        if (recipientId == null) {
            errorMessage = "Player '" + toName + "' not found (must be online)";
            return;
        }

        try {
            UUID senderId = client.player.getUuid();
            String senderName = client.player.getGameProfile().name();
            MailMessage msg = new MailMessage(0, recipientId, senderId, senderName, subject, body, false, 0);
            MossManApi.getSendMailUseCase().execute(msg);
            client.setScreen(parent);
        } catch (Exception e) {
            errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        }
    }

    private UUID resolvePlayerUuid(String name) {
        if (client.getNetworkHandler() == null) return null;
        Optional<PlayerListEntry> entry = client.getNetworkHandler().getPlayerList().stream()
                .filter(p -> p.getProfile().name().equalsIgnoreCase(name))
                .findFirst();
        return entry.map(p -> p.getProfile().id()).orElse(null);
    }
}
