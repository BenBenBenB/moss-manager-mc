package com.mossman;

import com.mossman.adapters.commands.MossManCommand;
import com.mossman.adapters.tui.TuiHelper;
import com.mossman.domain.entities.Comment;
import com.mossman.domain.entities.MailMessage;
import com.mossman.domain.events.TicketAssignedEvent;
import com.mossman.domain.events.TicketCommentedEvent;
import com.mossman.domain.events.TicketUpdatedEvent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class MossManTuiMod implements ModInitializer {
    public static final String MOD_ID = "mossman-tui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static MinecraftServer server;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MossManCommand.register(dispatcher);
        });

        LOGGER.info("MossMan TUI commands registered.");

        // Notify new assignee via mail
        MossManApi.getEventBus().subscribe(TicketAssignedEvent.class, event -> {
            try {
                var project = MossManApi.getProjectRepository().findById(event.ticket().getProjectId()).orElse(null);
                String key = event.ticket().getUserFriendlyKey(project != null ? project.getTicketPrefix() : "?");
                MailMessage saved = MossManApi.getSendMailUseCase().execute(new MailMessage(0, event.assigneeId(), null, "MossMan",
                        "Assigned to [" + key + "]",
                        "You have been assigned to [" + key + "]: " + event.ticket().getTitle(),
                        false, System.currentTimeMillis()));
                if (server != null) deliverMailNow(saved, server);
            } catch (Exception e) {
                LOGGER.error("Failed to send assignment notification", e);
            }
        });

        // Notify observers on ticket update (excluding the requester)
        MossManApi.getEventBus().subscribe(TicketUpdatedEvent.class, event -> {
            try {
                if (event.ticket().getObservers().isEmpty()) return;
                var project = MossManApi.getProjectRepository().findById(event.ticket().getProjectId()).orElse(null);
                String key = event.ticket().getUserFriendlyKey(project != null ? project.getTicketPrefix() : "?");
                String subject = "[" + key + "] was updated";
                String body = "Ticket [" + key + "]: " + event.ticket().getTitle() + " has been updated.";
                for (UUID observerId : event.ticket().getObservers()) {
                    if (observerId.equals(event.requesterId())) continue;
                    MailMessage saved = MossManApi.getSendMailUseCase().execute(new MailMessage(0, observerId, null, "MossMan",
                            subject, body, false, System.currentTimeMillis()));
                    if (server != null) deliverMailNow(saved, server);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to send update notification", e);
            }
        });

        // Notify observers when a comment is added (excluding the commenter)
        MossManApi.getEventBus().subscribe(TicketCommentedEvent.class, event -> {
            try {
                if (event.ticket().getObservers().isEmpty()) return;
                var project = MossManApi.getProjectRepository().findById(event.ticket().getProjectId()).orElse(null);
                String key = event.ticket().getUserFriendlyKey(project != null ? project.getTicketPrefix() : "?");
                String subject = "[" + key + "] new comment";
                Comment comment = event.comment();
                String body = comment.authorName() + " commented on [" + key + "]: " + event.ticket().getTitle()
                        + "\n\n" + comment.message();
                for (UUID observerId : event.ticket().getObservers()) {
                    if (observerId.equals(event.requesterId())) continue;
                    MailMessage saved = MossManApi.getSendMailUseCase().execute(new MailMessage(0, observerId, event.requesterId(),
                            comment.authorName(), subject, body, false, System.currentTimeMillis()));
                    if (server != null) deliverMailNow(saved, server);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to send comment notification", e);
            }
        });

        // Notify player of unread mail on join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, joinServer) -> {
            server = joinServer;
            var mailRepo = MossManApi.getMailRepository();
            if (mailRepo == null) return;
            UUID playerId = handler.player.getUuid();
            long unread = mailRepo.countUnread(playerId);
            if (unread > 0) {
                MutableText msg = Text.literal("MossMan: You have " + unread
                        + " unread message" + (unread == 1 ? "" : "s") + ". ")
                        .formatted(Formatting.GOLD)
                        .append(TuiHelper.createRunLink("[View]", "/mossman mail",
                                "Open your MossMan inbox", Formatting.GREEN));
                handler.player.sendMessage(msg);
            }
        });
    }

    public static void deliverMailNow(MailMessage saved, MinecraftServer srv) {
        ServerPlayerEntity player = srv.getPlayerManager().getPlayer(saved.recipientId());
        if (player == null) return;
        MutableText notification = Text.empty()
                .append(TuiHelper.createRunLink("[#" + saved.id() + "]",
                        "/mossman mail read " + saved.id(),
                        "Open message", Formatting.GOLD))
                .append(Text.literal(" " + saved.senderName() + ": " + saved.subject())
                        .formatted(Formatting.GOLD));
        player.sendMessage(notification);
    }
}
