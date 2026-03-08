package com.mossman.gui.screens;

import com.mossman.MossManApi;
import com.mossman.domain.entities.MailMessage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class ReadMailScreen extends Screen {

    private static final int HEADER_HEIGHT = 80;
    private static final int FOOTER_HEIGHT = 36;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final Screen parent;
    private final MailMessage message;

    public ReadMailScreen(Screen parent, MailMessage message) {
        super(Text.literal(message.subject()));
        this.parent = parent;
        this.message = message;
    }

    @Override
    protected void init() {
        // Mark as read on open
        UUID playerId = client.player != null ? client.player.getUuid() : null;
        if (playerId != null && !message.isRead()) {
            try {
                MossManApi.getMarkMailReadUseCase().execute(message.id(), playerId);
            } catch (Exception ignored) {}
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Reply"),
                btn -> openReply())
                .dimensions(width / 2 - 104, height - 28, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> close())
                .dimensions(width / 2 + 4, height - 28, 100, 20).build());
    }

    private void openReply() {
        if (message.senderId() != null) {
            ComposeMailScreen compose = new ComposeMailScreen(this);
            compose.prefill(message.senderName(), "Re: " + message.subject(), "");
            client.setScreen(compose);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        String from = "From: " + (message.senderName() != null ? message.senderName() : "System");
        String date = "Date: " + DATE_FMT.format(new Date(message.sentAt()));

        context.drawTextWithShadow(textRenderer, Text.literal(from), 10, 10, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, Text.literal(date), 10, 22, 0xAAAAAA);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(message.subject()), width / 2, 36, 0xFFFFFF);

        // Draw body with line wrapping
        int bodyY = HEADER_HEIGHT;
        String body = message.body() != null ? message.body() : "";
        String[] lines = body.split("\n");
        for (String line : lines) {
            if (bodyY + 10 >= height - FOOTER_HEIGHT) break;
            // Simple word wrap at screen width - 20
            while (line.length() > 0) {
                int chars = textRenderer.trimToWidth(line, width - 20).length();
                if (chars == 0) break;
                context.drawTextWithShadow(textRenderer, Text.literal(line.substring(0, chars)),
                        10, bodyY, 0xFFFFFF);
                line = line.substring(chars).stripLeading();
                bodyY += 12;
                if (bodyY + 10 >= height - FOOTER_HEIGHT) break;
            }
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
