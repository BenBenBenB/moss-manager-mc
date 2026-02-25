package com.mossman.adapters.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mossman.adapters.tui.SuggestionHelper;
import com.mossman.adapters.tui.TuiHelper;
import com.mossman.domain.entities.MailMessage;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public class MailCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralCommandNode<ServerCommandSource> rootNode) {
        var mailNode = CommandManager.literal("mail")
                .executes(MailCommand::showInbox)
                .then(CommandManager.literal("list")
                        .executes(MailCommand::listMailPage1)
                        .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                .executes(MailCommand::listMail)))
                .then(CommandManager.literal("read")
                        .then(CommandManager.argument("id", LongArgumentType.longArg(1))
                                .suggests(SuggestionHelper::suggestInboxMessageIds)
                                .executes(MailCommand::readMail)))
                .then(CommandManager.literal("send")
                        .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                .then(CommandManager.argument("subject", StringArgumentType.string())
                                        .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                                .executes(MailCommand::sendMail)))))
                .build();

        rootNode.addChild(mailNode);
    }

    private static int showInbox(CommandContext<ServerCommandSource> context) {
        return doListMail(context, 1);
    }

    private static int listMailPage1(CommandContext<ServerCommandSource> context) {
        return doListMail(context, 1);
    }

    private static int listMail(CommandContext<ServerCommandSource> context) {
        int page = IntegerArgumentType.getInteger(context, "page");
        return doListMail(context, page);
    }

    private static int doListMail(CommandContext<ServerCommandSource> context, int page) {
        ServerCommandSource source = context.getSource();
        UUID playerId = source.getPlayer() != null ? source.getPlayer().getUuid() : null;
        if (playerId == null) {
            source.sendMessage(Text.literal("Only players can use the mail command.").formatted(Formatting.RED));
            return 0;
        }

        int pageSize = 10;
        int offset = (page - 1) * pageSize;

        long total = com.mossman.MossManMod.getMailRepository().countByRecipientId(playerId);
        long unread = com.mossman.MossManMod.getMailRepository().countUnread(playerId);
        int totalPages = (int) Math.max(1, Math.ceil((double) total / pageSize));

        source.sendMessage(Text.literal("--- Inbox (" + unread + " unread) ---").formatted(Formatting.AQUA));

        List<MailMessage> messages = com.mossman.MossManMod.getMailRepository().findByRecipientId(playerId, offset, pageSize);

        if (messages.isEmpty()) {
            source.sendMessage(Text.literal("No messages.").formatted(Formatting.GRAY));
        } else {
            for (MailMessage msg : messages) {
                Formatting color = msg.isRead() ? Formatting.GRAY : Formatting.GOLD;
                MutableText line = Text.empty()
                        .append(TuiHelper.createRunLink("[#" + msg.id() + "]",
                                "/mossman mail read " + msg.id(),
                                "Read this message", color))
                        .append(Text.literal(" " + msg.senderName() + ": " + msg.subject()).formatted(color));
                source.sendMessage(line);
            }
        }

        TuiHelper.sendPaginationFooter(source, page, totalPages, "/mossman mail list");

        return 1;
    }

    private static int readMail(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        UUID playerId = source.getPlayer() != null ? source.getPlayer().getUuid() : null;
        if (playerId == null) {
            source.sendMessage(Text.literal("Only players can use the mail command.").formatted(Formatting.RED));
            return 0;
        }

        long id = LongArgumentType.getLong(context, "id");

        try {
            MailMessage msg = com.mossman.MossManMod.getMarkMailReadUseCase().execute(id, playerId);
            ZoneId zone = TuiHelper.resolveZone(source);
            source.sendMessage(Text.literal("--- Message #" + msg.id() + " ---").formatted(Formatting.AQUA));
            source.sendMessage(Text.literal("From: " + msg.senderName()).formatted(Formatting.GRAY));
            source.sendMessage(Text.literal("Sent: " + TuiHelper.formatTimestamp(msg.sentAt(), zone)).formatted(Formatting.GRAY));
            source.sendMessage(Text.literal("Subject: " + msg.subject()).formatted(Formatting.WHITE));
            source.sendMessage(Text.literal(msg.body()).formatted(Formatting.WHITE));
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }

    private static int sendMail(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        UUID senderId = source.getPlayer() != null ? source.getPlayer().getUuid() : null;
        String senderName = source.getPlayer() != null ? source.getPlayer().getName().getString() : "Console";

        try {
            String subject = StringArgumentType.getString(context, "subject");
            String messageText = StringArgumentType.getString(context, "message");

            for (var profile : GameProfileArgumentType.getProfileArgument(context, "player")) {
                UUID recipientId = profile.id();
                MailMessage msg = new MailMessage(0, recipientId, senderId, senderName, subject, messageText, false, 0);
                MailMessage saved = com.mossman.MossManMod.getSendMailUseCase().execute(msg);

                // Deliver to online player immediately
                com.mossman.MossManMod.deliverMailNow(saved, source.getServer());

                source.sendMessage(Text.literal("Message sent to " + profile.name()).formatted(Formatting.GREEN));
            }
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }
}
