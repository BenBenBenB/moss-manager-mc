package com.mossman.gui.screens;

import com.mossman.MossManApi;
import com.mossman.domain.entities.MailMessage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MailInboxScreen extends Screen {

    private static final int HEADER_HEIGHT = 32;
    private static final int FOOTER_HEIGHT = 36;
    private static final int ITEM_HEIGHT = 36;

    private final Screen parent;
    private String loadError;
    private long unreadCount = 0;

    public MailInboxScreen(Screen parent) {
        super(Text.literal("Inbox"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        loadError = null;
        UUID playerId = client.player != null ? client.player.getUuid() : null;

        List<MailMessage> messages = List.of();
        if (playerId != null) {
            try {
                messages = MossManApi.getMailRepository().findByRecipientId(playerId, 0, 200);
                unreadCount = MossManApi.getMailRepository().countUnread(playerId);
            } catch (Exception e) {
                loadError = "Failed to load mail: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }
        }

        MailListWidget mailList = new MailListWidget(
                client, width, height - HEADER_HEIGHT - FOOTER_HEIGHT, HEADER_HEIGHT, ITEM_HEIGHT, messages);
        addDrawableChild(mailList);

        addDrawableChild(ButtonWidget.builder(Text.literal("Compose"),
                btn -> client.setScreen(new ComposeMailScreen(this)))
                .dimensions(width / 2 - 104, height - 28, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> close())
                .dimensions(width / 2 + 4, height - 28, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        String titleStr = unreadCount > 0 ? "Inbox (" + unreadCount + " unread)" : "Inbox";
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(titleStr), width / 2, 12, 0xFFFFFF);
        if (loadError != null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(loadError),
                    width / 2, HEADER_HEIGHT + 4, 0xFF4444);
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private class MailListWidget extends AlwaysSelectedEntryListWidget<MailListWidget.MailEntry> {

        public MailListWidget(MinecraftClient client, int width, int height, int y,
                              int itemHeight, List<MailMessage> messages) {
            super(client, width, height, y, itemHeight);
            for (MailMessage msg : messages) {
                addEntry(new MailEntry(msg));
            }
            initialized = true;
        }

        @Override
        public int getRowWidth() {
            return MailInboxScreen.this.width - 20;
        }

        @Override
        protected int getScrollbarX() {
            return MailInboxScreen.this.width / 2 + getRowWidth() / 2 + 4;
        }

        private boolean initialized = false;

        @Override
        public void setSelected(MailEntry entry) {
            super.setSelected(entry);
            if (initialized && entry != null) {
                MailInboxScreen.this.client.setScreen(new ReadMailScreen(MailInboxScreen.this, entry.message));
            }
        }

        public class MailEntry extends AlwaysSelectedEntryListWidget.Entry<MailEntry> {
            final MailMessage message;
            private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("MM/dd HH:mm");

            public MailEntry(MailMessage message) {
                this.message = message;
            }

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int nameColor = message.isRead() ? 0xAAAAAA : 0xFFAA00;
                int subjectColor = message.isRead() ? 0xCCCCCC : 0xFFFFFF;
                String from = "From: " + (message.senderName() != null ? message.senderName() : "System");
                String date = DATE_FMT.format(new Date(message.sentAt()));
                context.drawTextWithShadow(MailInboxScreen.this.textRenderer,
                        Text.literal(from + "  " + date), getX(), getY() + 2, nameColor);
                context.drawTextWithShadow(MailInboxScreen.this.textRenderer,
                        Text.literal(message.subject()), getX(), getY() + 14, subjectColor);
                if (!message.isRead()) {
                    context.drawTextWithShadow(MailInboxScreen.this.textRenderer,
                            Text.literal("\u2022"), getX() + getRowWidth() - 10, getY() + 7, 0xFFAA00);
                }
            }

            @Override
            public Text getNarration() {
                return Text.literal(message.subject());
            }
        }
    }
}
