package com.mossman.adapters.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mossman.adapters.tui.DurationParser;
import com.mossman.adapters.tui.NbtPatchParser;
import com.mossman.adapters.tui.ProjectSuggestions;
import com.mossman.adapters.tui.SuggestionHelper;
import com.mossman.adapters.tui.TicketSuggestions;
import com.mossman.adapters.tui.TuiHelper;
import com.mossman.domain.query.TicketFilter;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import com.mossman.domain.entities.TicketRelationship;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class TicketCommand {

    private record TicketResolution(com.mossman.domain.entities.Project project, com.mossman.domain.entities.Ticket ticket, String prefix) {}

    /** Resolves a "PREFIX-N" key to its project and ticket.
     *  Sends the appropriate error message and returns empty on failure. */
    private static java.util.Optional<TicketResolution> resolveTicket(
            ServerCommandSource source, String key) {
        String[] parts = key.split("-", 2);
        if (parts.length != 2) {
            source.sendMessage(Text.literal("Invalid ticket key. Expected PREFIX-NUMBER")
                    .formatted(Formatting.RED));
            return java.util.Optional.empty();
        }
        String prefix = parts[0];
        int number;
        try { number = Integer.parseInt(parts[1]); }
        catch (NumberFormatException e) {
            source.sendMessage(Text.literal("Invalid ticket key: " + key).formatted(Formatting.RED));
            return java.util.Optional.empty();
        }
        var projectOpt = com.mossman.MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst();
        if (projectOpt.isEmpty()) {
            source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED));
            return java.util.Optional.empty();
        }
        var project = projectOpt.get();
        if (!com.mossman.domain.auth.PermissionChecker.canView(project, source)) {
            source.sendMessage(Text.literal("You do not have permission to view this project.")
                    .formatted(Formatting.RED));
            return java.util.Optional.empty();
        }
        var ticketOpt = com.mossman.MossManMod.getTicketRepository()
                .findByProjectId(project.getId(), 0, Integer.MAX_VALUE).stream()
                .filter(t -> t.getTicketNumber() == number).findFirst();
        if (ticketOpt.isEmpty()) {
            source.sendMessage(Text.literal("Ticket not found: " + key).formatted(Formatting.RED));
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new TicketResolution(project, ticketOpt.get(), prefix));
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralCommandNode<ServerCommandSource> rootNode) {
        var ticketNode = CommandManager.literal("ticket")
                .then(CommandManager.literal("list")
                        .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                .executes(TicketCommand::listTickets)
                                .then(CommandManager.argument("page", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                        .executes(TicketCommand::listTickets))
                                .then(CommandManager.argument("filter", NbtCompoundArgumentType.nbtCompound())
                                        .suggests(TicketSuggestions.suggestTicketFilter("prefix"))
                                        .executes(TicketCommand::listTicketsFiltered)
                                        .then(CommandManager.argument("page", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                                .executes(TicketCommand::listTicketsFiltered)))))
                .then(CommandManager.literal("view")
                        .then(CommandManager.argument("key", StringArgumentType.word()).suggests(TicketSuggestions::suggestTicketKeys)
                                .executes(TicketCommand::viewTicket)))
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                .then(CommandManager.argument("title", StringArgumentType.greedyString())
                                        .executes(TicketCommand::createTicket))))
                .then(CommandManager.literal("update")
                        .then(CommandManager.argument("key", StringArgumentType.word()).suggests(TicketSuggestions::suggestTicketKeys)
                                .then(CommandManager.argument("patch", NbtCompoundArgumentType.nbtCompound())
                                        .suggests(TicketSuggestions.suggestTicketPatch("key"))
                                        .executes(TicketCommand::updateTicket))))
                .then(CommandManager.literal("comment")
                        .then(CommandManager.literal("delete")
                                .then(CommandManager.argument("commentId", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                        .executes(TicketCommand::deleteComment)))
                        .then(CommandManager.argument("key", StringArgumentType.word()).suggests(TicketSuggestions::suggestTicketKeys)
                                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                        .executes(TicketCommand::addComment))))
                .then(CommandManager.literal("assign")
                        .then(CommandManager.argument("key", StringArgumentType.word()).suggests(TicketSuggestions::suggestTicketKeys)
                                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                        .executes(TicketCommand::assignTicket))))
                .then(CommandManager.literal("unassign")
                        .then(CommandManager.argument("key", StringArgumentType.word()).suggests(TicketSuggestions::suggestTicketKeys)
                                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                        .suggests(TicketSuggestions.suggestTicketAssignees("key"))
                                        .executes(TicketCommand::unassignTicket))))
                .then(CommandManager.literal("comments")
                        .then(CommandManager.argument("key", StringArgumentType.word()).suggests(TicketSuggestions::suggestTicketKeys)
                                .executes(TicketCommand::listComments)
                                .then(CommandManager.argument("page", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                        .executes(TicketCommand::listComments))))
                .then(CommandManager.literal("log")
                        .then(CommandManager.literal("delete")
                                .then(CommandManager.argument("logId", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                        .executes(TicketCommand::deleteTimeLog)))
                        .then(CommandManager.argument("key", StringArgumentType.word()).suggests(TicketSuggestions::suggestTicketKeys)
                                .then(CommandManager.argument("input", StringArgumentType.greedyString())
                                        .executes(TicketCommand::addTimeLog))))
                .then(CommandManager.literal("logs")
                        .then(CommandManager.argument("key", StringArgumentType.word()).suggests(TicketSuggestions::suggestTicketKeys)
                                .executes(TicketCommand::listTimeLogs)
                                .then(CommandManager.argument("page", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                        .executes(TicketCommand::listTimeLogs))))
                .then(CommandManager.literal("watch")
                        .then(CommandManager.argument("key", StringArgumentType.word())
                                .suggests(TicketSuggestions::suggestTicketKeys)
                                .executes(TicketCommand::watchTicket)))
                .then(CommandManager.literal("unwatch")
                        .then(CommandManager.argument("key", StringArgumentType.word())
                                .suggests(TicketSuggestions::suggestTicketKeys)
                                .executes(TicketCommand::unwatchTicket)))
                .then(CommandManager.literal("link")
                        .then(CommandManager.argument("sourceKey", StringArgumentType.word())
                                .suggests(TicketSuggestions::suggestTicketKeys)
                                .then(CommandManager.argument("type", StringArgumentType.word())
                                        .suggests(ProjectSuggestions.suggestRelationshipTypeNamesForKey("sourceKey"))
                                        .then(CommandManager.argument("targetKey", StringArgumentType.word())
                                                .suggests(TicketSuggestions::suggestTicketKeys)
                                                .executes(TicketCommand::linkTickets)))))
                .then(CommandManager.literal("unlink")
                        .then(CommandManager.argument("sourceKey", StringArgumentType.word())
                                .suggests(TicketSuggestions::suggestTicketKeys)
                                .then(CommandManager.argument("type", StringArgumentType.word())
                                        .suggests(ProjectSuggestions.suggestRelationshipTypeNamesForKey("sourceKey"))
                                        .then(CommandManager.argument("targetKey", StringArgumentType.word())
                                                .suggests(TicketSuggestions::suggestTicketKeys)
                                                .executes(TicketCommand::unlinkTickets)))))
                .build();

        rootNode.addChild(ticketNode);
    }

    private static int listTickets(CommandContext<ServerCommandSource> context) {
        int page = getPage(context);
        return doListTickets(context, TicketFilter.empty(), page);
    }

    private static int listTicketsFiltered(CommandContext<ServerCommandSource> context) {
        int page = getPage(context);
        NbtCompound nbt = NbtCompoundArgumentType.getNbtCompound(context, "filter");
        Map<String, String> filterMap = new HashMap<>();
        // Supported SNBT string keys: status, type, priority, title
        // Supported SNBT list key: label (e.g. {label:["bug","feature"]})
        for (String key : nbt.getKeys()) {
            nbt.getString(key).ifPresent(value -> filterMap.put(key.toLowerCase(), value));
        }
        List<String> filterLabels = extractStringList(nbt, "label");
        return doListTickets(context, TicketFilter.of(filterMap, filterLabels), page);
    }

    /** Extracts a list of strings from an NBT list element in the given compound. Returns empty list if absent or wrong type. */
    private static List<String> extractStringList(NbtCompound nbt, String key) {
        net.minecraft.nbt.NbtElement element = nbt.get(key);
        if (!(element instanceof net.minecraft.nbt.NbtList list)) return List.of();
        List<String> result = new ArrayList<>();
        for (net.minecraft.nbt.NbtElement e : list) {
            if (e.getType() == net.minecraft.nbt.NbtElement.STRING_TYPE) {
                e.asString().filter(s -> !s.isBlank()).ifPresent(result::add);
            }
        }
        return result;
    }

    private static int getPage(CommandContext<ServerCommandSource> context) {
        return TuiHelper.getOptionalPage(context);
    }

    private static int doListTickets(CommandContext<ServerCommandSource> context, TicketFilter filter, int page) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        
        var projectOpt = com.mossman.MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                .findFirst();

        if (projectOpt.isEmpty()) {
            source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED));
            return 0;
        }

        var project = projectOpt.get();
        if (!com.mossman.domain.auth.PermissionChecker.canView(project, source)) {
            source.sendMessage(Text.literal("You do not have permission to view tickets for this project.").formatted(Formatting.RED));
            return 0;
        }

        var headerText = filter.hasAny()
                ? TuiHelper.translatable("mossman.tui.ticket.list.header", prefix).getString() + " [filtered]"
                : TuiHelper.translatable("mossman.tui.ticket.list.header", prefix).getString();
        source.sendMessage(Text.literal(headerText).formatted(Formatting.AQUA));
        
        int pageSize = 10;
        int offset = (page - 1) * pageSize;
        
        var tickets = filter.hasAny()
                ? com.mossman.MossManMod.getTicketRepository().findByProjectId(project.getId(), filter, offset, pageSize)
                : com.mossman.MossManMod.getTicketRepository().findByProjectId(project.getId(), offset, pageSize);
        
        long totalTickets = filter.hasAny()
                ? com.mossman.MossManMod.getTicketRepository().countByProjectId(project.getId(), filter)
                : com.mossman.MossManMod.getTicketRepository().countByProjectId(project.getId());
        int totalPages = (int) Math.ceil((double) totalTickets / pageSize);

        if (tickets.isEmpty()) {
            source.sendMessage(Text.literal("No tickets found.").formatted(Formatting.GRAY));
        } else {
            for (var ticket : tickets) {
                String key = ticket.getUserFriendlyKey(prefix);
                MutableText ticketLink = TuiHelper.createRunLink(
                        "[" + key + "]", 
                        "/mossman ticket view " + key, 
                        TuiHelper.translatable("mossman.tui.ticket.view_hover").getString(), 
                        Formatting.GREEN
                ).append(Text.literal(" " + ticket.getTitle() + " [" + ticket.getStatus() + "]").formatted(Formatting.WHITE));
                source.sendMessage(ticketLink);
            }
        }
        
        // Pagination footer
        if (totalPages > 1) {
            MutableText nav = Text.empty();
            if (page > 1) {
                String prevCmd = filter.hasAny() 
                    ? String.format("/mossman ticket list %s %s %d", prefix, getFilterNbt(context), page - 1)
                    : String.format("/mossman ticket list %s %d", prefix, page - 1);
                nav.append(TuiHelper.createRunLink(TuiHelper.translatable("mossman.tui.common.pagination.prev").getString(), prevCmd, "Previous Page", Formatting.GOLD)).append(" ");
            }
            nav.append(TuiHelper.translatable("mossman.tui.common.pagination.page_info", page, totalPages).formatted(Formatting.GRAY));
            if (page < totalPages) {
                String nextCmd = filter.hasAny()
                    ? String.format("/mossman ticket list %s %s %d", prefix, getFilterNbt(context), page + 1)
                    : String.format("/mossman ticket list %s %d", prefix, page + 1);
                nav.append(" ").append(TuiHelper.createRunLink(TuiHelper.translatable("mossman.tui.common.pagination.next").getString(), nextCmd, "Next Page", Formatting.GOLD));
            }
            source.sendMessage(nav);
        }
        
        source.sendMessage(TuiHelper.translatable("mossman.tui.ticket.list.footer").formatted(Formatting.GRAY));
        
        MutableText createBtn = TuiHelper.createSuggestLink(
                TuiHelper.translatable("mossman.tui.ticket.create_btn").getString(),
                "/mossman ticket create " + prefix + " ",
                TuiHelper.translatable("mossman.tui.ticket.create_hover").getString(),
                Formatting.GOLD
        );
        source.sendMessage(createBtn);

        return 1;
    }

    private static int listComments(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String key = StringArgumentType.getString(context, "key");
        int page = getPage(context);

        var r = resolveTicket(source, key);
        if (r.isEmpty()) return 0;
        var project = r.get().project();
        var ticket  = r.get().ticket();
        String prefix = r.get().prefix();

        int pageSize = 10;
        int offset = (page - 1) * pageSize;
        long totalComments = com.mossman.MossManMod.getCommentRepository().countByTicketId(ticket.getId());
        int totalPages = Math.max(1, (int) Math.ceil((double) totalComments / pageSize));

        source.sendMessage(Text.literal("--- Comments: " + key + " ---").formatted(Formatting.AQUA));

        List<com.mossman.domain.entities.Comment> comments = com.mossman.MossManMod.getCommentRepository()
                .findByTicketId(ticket.getId(), offset, pageSize);

        if (comments.isEmpty()) {
            source.sendMessage(Text.literal("No comments yet.").formatted(Formatting.GRAY));
        } else {
            ZoneId playerZone = TuiHelper.resolveZone(source);
            UUID currentPlayerId = source.getPlayer() != null ? source.getPlayer().getUuid() : null;
            boolean isEditor = com.mossman.domain.auth.PermissionChecker.hasPermission(project, source, com.mossman.domain.entities.Permission.EDITOR);
            for (var comment : comments) {
                String dateStr = TuiHelper.formatTimestamp(comment.createdAt(), playerZone);
                boolean canDelete = isEditor || (currentPlayerId != null && currentPlayerId.equals(comment.authorId()));
                MutableText commentLine = Text.literal("  ").formatted(Formatting.WHITE);
                if (canDelete) {
                    commentLine.append(TuiHelper.createRunLink("[✗]", "/mossman ticket comment delete " + comment.id(), "Delete comment", Formatting.RED));
                    commentLine.append(Text.literal(" ").formatted(Formatting.WHITE));
                }
                commentLine.append(Text.literal("[#" + comment.id() + "] ").formatted(Formatting.GOLD));
                commentLine.append(Text.literal(comment.authorName() + " (" + dateStr + "): ").formatted(Formatting.GRAY));
                commentLine.append(Text.literal(comment.message()).formatted(Formatting.WHITE));
                source.sendMessage(commentLine);
            }
        }

        TuiHelper.sendPaginationFooter(source, page, totalPages, "/mossman ticket comments " + key);

        source.sendMessage(TuiHelper.createRunLink("[← Back to ticket]", "/mossman ticket view " + key, "View ticket", Formatting.GRAY));
        return 1;
    }

    private static int viewTicket(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String key = StringArgumentType.getString(context, "key");

        var r = resolveTicket(source, key);
        if (r.isEmpty()) return 0;
        var project = r.get().project();
        var ticket  = r.get().ticket();
        String prefix = r.get().prefix();
        source.sendMessage(Text.literal("--- Ticket: " + key + " ---").formatted(Formatting.AQUA));
        
        boolean isEditor = com.mossman.domain.auth.PermissionChecker.hasPermission(project, source, com.mossman.domain.entities.Permission.EDITOR);
        
        // Title
        MutableText titleLine = Text.empty();
        if (isEditor) {
            titleLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman ticket update " + key + " {title:\"" + ticket.getTitle() + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Title"), Formatting.GRAY));
        }
        titleLine.append(Text.literal("Title: " + ticket.getTitle()).formatted(Formatting.WHITE));
        source.sendMessage(titleLine);

        // Status
        MutableText statusLine = Text.empty();
        if (isEditor) {
            statusLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman ticket update " + key + " {status:\"" + ticket.getStatus() + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Status"), Formatting.GRAY));
        }
        statusLine.append(Text.literal("Status: " + ticket.getStatus()).formatted(Formatting.YELLOW));
        source.sendMessage(statusLine);

        // Labels
        String labelsSnbt = "{labels:[" + ticket.getLabels().stream()
                .map(l -> "\"" + l + "\"").collect(Collectors.joining(",")) + "]}";
        MutableText labelsLine = Text.empty();
        if (isEditor) {
            labelsLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman ticket update " + key + " " + labelsSnbt,
                    TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Labels"), Formatting.GRAY));
        }
        if (ticket.getLabels().isEmpty()) {
            labelsLine.append(Text.literal("Labels: None").formatted(Formatting.GRAY));
        } else {
            labelsLine.append(Text.literal("Labels: ").formatted(Formatting.GRAY));
            labelsLine.append(Text.literal(String.join(", ", ticket.getLabels())).formatted(Formatting.YELLOW));
        }
        source.sendMessage(labelsLine);

        // Description
        MutableText descLine = Text.empty();
        if (isEditor) {
            descLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman ticket update " + key + " {description:\"" + (ticket.getDescription() != null ? ticket.getDescription() : "") + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Description"), Formatting.GRAY));
        }
        descLine.append(Text.literal("Description: " + (ticket.getDescription() != null ? ticket.getDescription() : "None")).formatted(Formatting.GRAY));
        source.sendMessage(descLine);

        // Assignees
        MutableText assigneesLine = Text.literal("Assignees: ").formatted(Formatting.GRAY);
        if (ticket.getAssignees().isEmpty()) {
            assigneesLine.append(Text.literal("None").formatted(Formatting.GRAY));
        } else {
            for (UUID assigneeId : ticket.getAssignees()) {
                String username = resolveUsername(assigneeId, project, source);
                assigneesLine.append(Text.literal(username).formatted(Formatting.WHITE));
                if (isEditor) {
                    assigneesLine.append(Text.literal(" ").formatted(Formatting.WHITE));
                    assigneesLine.append(TuiHelper.createRunLink("[✗]", "/mossman ticket unassign " + key + " " + username, "Unassign " + username, Formatting.RED));
                }
                assigneesLine.append(Text.literal(" ").formatted(Formatting.WHITE));
            }
        }
        source.sendMessage(assigneesLine);
        if (isEditor) {
            source.sendMessage(TuiHelper.createSuggestLink("[+ Assign] ", "/mossman ticket assign " + key + " ", "Assign a player", Formatting.GOLD));
        }

        // Observers
        UUID currentPlayerId = source.getPlayer() != null ? source.getPlayer().getUuid() : null;
        MutableText observersLine = Text.literal("Observers: ").formatted(Formatting.GRAY);
        if (ticket.getObservers().isEmpty()) {
            observersLine.append(Text.literal("None").formatted(Formatting.GRAY));
        } else {
            for (UUID observerId : ticket.getObservers()) {
                String username = resolveUsername(observerId, project, source);
                observersLine.append(Text.literal(username + " ").formatted(Formatting.WHITE));
            }
        }
        source.sendMessage(observersLine);
        if (currentPlayerId != null) {
            if (ticket.getObservers().contains(currentPlayerId)) {
                source.sendMessage(TuiHelper.createRunLink("[✗ Unwatch]", "/mossman ticket unwatch " + key, "Stop watching this ticket", Formatting.RED));
            } else {
                source.sendMessage(TuiHelper.createRunLink("[+ Watch]", "/mossman ticket watch " + key, "Watch this ticket", Formatting.GOLD));
            }
        }

        // Relationships — only show section if project has relationship types defined or ticket has existing relationships
        List<TicketRelationship> rels = com.mossman.MossManMod.getTicketRelationshipRepository().findByTicketId(ticket.getId());
        boolean hasRelTypes = !project.getRelationshipTypes().isEmpty();
        if (!rels.isEmpty() || hasRelTypes) {
            MutableText relsHeader = Text.literal("Relationships: ").formatted(Formatting.GRAY);
            if (rels.isEmpty()) {
                relsHeader.append(Text.literal("None").formatted(Formatting.GRAY));
            }
            source.sendMessage(relsHeader);
            for (TicketRelationship rel : rels) {
                var relTypeOpt = project.getRelationshipTypes().stream()
                        .filter(rt -> rt.key().equalsIgnoreCase(rel.type()))
                        .findFirst();
                if (relTypeOpt.isEmpty()) continue;
                var relType = relTypeOpt.get();

                boolean isSource = rel.sourceTicketId() == ticket.getId();
                long partnerTicketId = isSource ? rel.targetTicketId() : rel.sourceTicketId();
                String direction = isSource ? relType.sourceToTargetDescription() : relType.targetToSourceDescription();
                String arrow = isSource ? "→" : "←";

                var partnerTicketOpt = com.mossman.MossManMod.getTicketRepository().findById(partnerTicketId);
                if (partnerTicketOpt.isEmpty()) continue;
                var partnerTicket = partnerTicketOpt.get();
                String partnerKey = partnerTicket.getUserFriendlyKey(prefix);

                String sourceKey = isSource ? key : partnerKey;
                String targetKey = isSource ? partnerKey : key;
                String unlinkCmd = "/mossman ticket unlink " + sourceKey + " " + rel.type() + " " + targetKey;
                String viewPartnerCmd = "/mossman ticket view " + partnerKey;

                MutableText relLine = Text.literal("  ").formatted(Formatting.WHITE);
                relLine.append(TuiHelper.createRunLink("[✗]", unlinkCmd, "Unlink", Formatting.RED));
                relLine.append(TuiHelper.applyColor(Text.literal(" " + direction + " " + arrow + " ").formatted(Formatting.GRAY), relType.textColor()));
                relLine.append(TuiHelper.createRunLink(partnerKey, viewPartnerCmd, "View " + partnerKey, Formatting.GREEN));
                relLine.append(Text.literal(": " + partnerTicket.getTitle()).formatted(Formatting.WHITE));
                source.sendMessage(relLine);
            }
            if (isEditor && hasRelTypes) {
                source.sendMessage(TuiHelper.createSuggestLink("[+ Link]", "/mossman ticket link " + key + " ", "Link to another ticket", Formatting.GOLD));
            }
        }

        // Comments
        long commentCount = com.mossman.MossManMod.getCommentRepository().countByTicketId(ticket.getId());
        ZoneId playerZone = TuiHelper.resolveZone(source);
        MutableText commentsHeader = Text.empty();
        commentsHeader.append(Text.literal("Comments (" + commentCount + "): ").formatted(Formatting.GRAY));
        if (commentCount > 0) {
            commentsHeader.append(TuiHelper.createRunLink("[View all →]", "/mossman ticket comments " + key, "View all comments", Formatting.GOLD));
        }
        source.sendMessage(commentsHeader);
        if (commentCount > 0) {
            var latestList = com.mossman.MossManMod.getCommentRepository().findByTicketId(ticket.getId(), (int) (commentCount - 1), 1);
            if (!latestList.isEmpty()) {
                var comment = latestList.get(0);
                String dateStr = TuiHelper.formatTimestamp(comment.createdAt(), playerZone);
                boolean canDelete = isEditor || (currentPlayerId != null && currentPlayerId.equals(comment.authorId()));
                MutableText commentLine = Text.literal("  ").formatted(Formatting.WHITE);
                if (canDelete) {
                    commentLine.append(TuiHelper.createRunLink("[✗]", "/mossman ticket comment delete " + comment.id(), "Delete comment", Formatting.RED));
                    commentLine.append(Text.literal(" ").formatted(Formatting.WHITE));
                }
                commentLine.append(Text.literal("[#" + comment.id() + "] ").formatted(Formatting.GOLD));
                commentLine.append(Text.literal(comment.authorName() + " (" + dateStr + "): ").formatted(Formatting.GRAY));
                commentLine.append(Text.literal(comment.message()).formatted(Formatting.WHITE));
                source.sendMessage(commentLine);
                if (commentCount > 1) {
                    source.sendMessage(Text.literal("  (" + (commentCount - 1) + " older comment" + (commentCount > 2 ? "s" : "") + " hidden)").formatted(Formatting.DARK_GRAY));
                }
            }
        }
        if (com.mossman.domain.auth.PermissionChecker.hasPermission(project, source, com.mossman.domain.entities.Permission.CREATOR)) {
            source.sendMessage(TuiHelper.createSuggestLink("[+ Comment]", "/mossman ticket comment " + key + " ", "Add a comment", Formatting.GOLD));
        }

        // Time Logs
        long logCount = com.mossman.MossManMod.getTimeLogRepository().countByTicketId(ticket.getId());
        long totalMinutes = com.mossman.MossManMod.getTimeLogRepository().sumMinutesByTicketId(ticket.getId());
        String totalFormatted = DurationParser.format(totalMinutes);
        MutableText logsHeader = Text.empty();
        logsHeader.append(Text.literal("Time Logged (" + totalFormatted + ", " + logCount + " " + (logCount == 1 ? "entry" : "entries") + "): ").formatted(Formatting.GRAY));
        if (logCount > 0) {
            logsHeader.append(TuiHelper.createRunLink("[View all →]", "/mossman ticket logs " + key, "View all time logs", Formatting.GOLD));
        }
        source.sendMessage(logsHeader);
        if (logCount > 0) {
            var latestLogs = com.mossman.MossManMod.getTimeLogRepository().findByTicketId(ticket.getId(), 0, 1);
            if (!latestLogs.isEmpty()) {
                var tlog = latestLogs.get(0);
                String dateStr = TuiHelper.formatTimestamp(tlog.loggedAt(), playerZone);
                boolean canDeleteLog = isEditor || (currentPlayerId != null && currentPlayerId.equals(tlog.workerId()));
                MutableText logLine = Text.literal("  ").formatted(Formatting.WHITE);
                if (canDeleteLog) {
                    logLine.append(TuiHelper.createRunLink("[✗]", "/mossman ticket log delete " + tlog.id(), "Delete time log", Formatting.RED));
                    logLine.append(Text.literal(" ").formatted(Formatting.WHITE));
                }
                logLine.append(Text.literal("[#" + tlog.id() + "] ").formatted(Formatting.GOLD));
                logLine.append(Text.literal(tlog.workerName() + " (" + dateStr + "): ").formatted(Formatting.GRAY));
                logLine.append(Text.literal(DurationParser.format(tlog.minutes())).formatted(Formatting.WHITE));
                if (!tlog.note().isBlank()) {
                    logLine.append(Text.literal(" — " + tlog.note()).formatted(Formatting.GRAY));
                }
                source.sendMessage(logLine);
                if (logCount > 1) {
                    source.sendMessage(Text.literal("  (" + (logCount - 1) + " older " + (logCount > 2 ? "entries" : "entry") + " hidden)").formatted(Formatting.DARK_GRAY));
                }
            }
        }
        if (com.mossman.domain.auth.PermissionChecker.hasPermission(project, source, com.mossman.domain.entities.Permission.CREATOR)) {
            source.sendMessage(TuiHelper.createSuggestLink("[+ Log Time]", "/mossman ticket log " + key + " ", "Log time on this ticket", Formatting.GOLD));
        }

        if (com.mossman.domain.auth.PermissionChecker.hasPermission(project, source, com.mossman.domain.entities.Permission.EDITOR)) {
            MutableText editBtn = TuiHelper.createSuggestLink("[Edit] ", "/mossman ticket update " + key + " ", "Edit ticket fields", Formatting.YELLOW);
            source.sendMessage(editBtn);
        }

        return 1;
    }

    private static int createTicket(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String title = StringArgumentType.getString(context, "title");
        
        var projectOpt = com.mossman.MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                .findFirst();

        if (projectOpt.isEmpty()) {
            source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED));
            return 0;
        }

        try {
            var project = projectOpt.get();
            java.util.UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : java.util.UUID.randomUUID();
            int nextNumber = com.mossman.MossManMod.getTicketRepository().getNextTicketNumber(project.getId());
            
            var ticket = new com.mossman.domain.entities.Ticket(
                    0, project.getId(), nextNumber, title, "", "Task", "OPEN", 
                    com.mossman.domain.entities.Priority.MEDIUM, 
                    java.util.Collections.emptyList(), java.util.Collections.emptyList(), 
                    rId, java.util.Collections.emptyList(), 
                    System.currentTimeMillis(), System.currentTimeMillis(), null
            );

            var savedTicket = com.mossman.MossManMod.getCreateTicketUseCase().execute(ticket, rId);
            
            String key = savedTicket.getUserFriendlyKey(prefix);
            MutableText response = TuiHelper.translatable("mossman.tui.ticket.created", title).formatted(Formatting.GREEN);
            response.append(TuiHelper.createRunLink(
                    "[" + key + "]",
                    "/mossman ticket view " + key,
                    TuiHelper.translatable("mossman.tui.ticket.view_hover").getString(),
                    Formatting.GOLD
            ));
            source.sendMessage(response);
            
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }



    private static int addComment(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String key = StringArgumentType.getString(context, "key");
        String message = StringArgumentType.getString(context, "message");
        var r = resolveTicket(source, key);
        if (r.isEmpty()) return 0;
        var ticket = r.get().ticket();
        try {
            UUID requesterId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            String authorName = source.getPlayer() != null ? source.getPlayer().getName().getString() : "system";
            com.mossman.MossManMod.getAddCommentUseCase().execute(ticket.getId(), requesterId, authorName, message);
            source.sendMessage(Text.literal("Comment added to " + key + ".").formatted(Formatting.GREEN));
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }

    private static int deleteComment(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        long commentId = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "commentId");
        UUID requesterId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
        try {
            com.mossman.MossManMod.getDeleteCommentUseCase().execute(commentId, requesterId);
            source.sendMessage(Text.literal("Comment deleted.").formatted(Formatting.YELLOW));
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }

    private static int assignTicket(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String key = StringArgumentType.getString(context, "key");
        var r = resolveTicket(source, key);
        if (r.isEmpty()) return 0;
        var ticket = r.get().ticket();
        try {
            UUID requesterId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            for (var profile : GameProfileArgumentType.getProfileArgument(context, "player")) {
                com.mossman.MossManMod.getAssignTicketUseCase().execute(ticket.getId(), requesterId, profile.id());
                source.sendMessage(Text.literal("Assigned " + profile.name() + " to " + key).formatted(Formatting.GREEN));
            }
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }

    private static int unassignTicket(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String key = StringArgumentType.getString(context, "key");
        var r = resolveTicket(source, key);
        if (r.isEmpty()) return 0;
        var ticket = r.get().ticket();
        try {
            UUID requesterId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            for (var profile : GameProfileArgumentType.getProfileArgument(context, "player")) {
                com.mossman.MossManMod.getUnassignTicketUseCase().execute(ticket.getId(), requesterId, profile.id());
                source.sendMessage(Text.literal("Unassigned " + profile.name() + " from " + key).formatted(Formatting.GREEN));
            }
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }

    private static int watchTicket(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String key = StringArgumentType.getString(context, "key");
        var r = resolveTicket(source, key);
        if (r.isEmpty()) return 0;
        var ticket = r.get().ticket();
        try {
            UUID requesterId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            com.mossman.MossManMod.getObserveTicketUseCase().execute(ticket.getId(), requesterId);
            source.sendMessage(Text.literal("Now watching " + key).formatted(Formatting.GREEN));
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }

    private static int unwatchTicket(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String key = StringArgumentType.getString(context, "key");
        var r = resolveTicket(source, key);
        if (r.isEmpty()) return 0;
        var ticket = r.get().ticket();
        try {
            UUID requesterId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            com.mossman.MossManMod.getUnobserveTicketUseCase().execute(ticket.getId(), requesterId);
            source.sendMessage(Text.literal("Stopped watching " + key).formatted(Formatting.GREEN));
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }

    private static int linkTickets(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String sourceKey = StringArgumentType.getString(context, "sourceKey");
        String type = StringArgumentType.getString(context, "type");
        String targetKey = StringArgumentType.getString(context, "targetKey");

        var rs = resolveTicket(source, sourceKey);
        if (rs.isEmpty()) return 0;
        var rt = resolveTicket(source, targetKey);
        if (rt.isEmpty()) return 0;

        var sourceProject = rs.get().project();
        var sourceTicket  = rs.get().ticket();
        var targetTicket  = rt.get().ticket();

        try {
            UUID requesterId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            com.mossman.MossManMod.getLinkTicketsUseCase().execute(sourceTicket.getId(), targetTicket.getId(), type, requesterId);

            var relTypeOpt = sourceProject.getRelationshipTypes().stream()
                    .filter(r2 -> r2.key().equalsIgnoreCase(type)).findFirst();
            String desc = relTypeOpt.map(r2 -> r2.sourceToTargetDescription()).orElse(type);
            source.sendMessage(Text.literal("Linked " + sourceKey + " " + desc + " " + targetKey).formatted(Formatting.GREEN));
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }

    private static int unlinkTickets(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String sourceKey = StringArgumentType.getString(context, "sourceKey");
        String type = StringArgumentType.getString(context, "type");
        String targetKey = StringArgumentType.getString(context, "targetKey");

        var rs = resolveTicket(source, sourceKey);
        if (rs.isEmpty()) return 0;
        var rt = resolveTicket(source, targetKey);
        if (rt.isEmpty()) return 0;

        var sourceTicket = rs.get().ticket();
        var targetTicket = rt.get().ticket();

        try {
            UUID requesterId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            com.mossman.MossManMod.getUnlinkTicketsUseCase().execute(sourceTicket.getId(), targetTicket.getId(), type, requesterId);

            source.sendMessage(Text.literal("Unlinked " + sourceKey + " from " + targetKey).formatted(Formatting.YELLOW));
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }

    private static int addTimeLog(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String key = StringArgumentType.getString(context, "key");
        String input = StringArgumentType.getString(context, "input");
        String[] parts = DurationParser.splitInputAndNote(input);
        String durationStr = parts[0];
        String note = parts[1];
        var r = resolveTicket(source, key);
        if (r.isEmpty()) return 0;
        var ticket = r.get().ticket();
        try {
            UUID requesterId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            String workerName = source.getPlayer() != null ? source.getPlayer().getName().getString() : "system";
            var log = com.mossman.MossManMod.getLogTimeUseCase().execute(ticket.getId(), requesterId, workerName, durationStr, note);
            source.sendMessage(Text.literal("Logged " + DurationParser.format(log.minutes()) + " on " + key + ".").formatted(Formatting.GREEN));
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }

    private static int deleteTimeLog(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        long logId = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "logId");
        UUID requesterId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
        try {
            com.mossman.MossManMod.getDeleteTimeLogUseCase().execute(logId, requesterId);
            source.sendMessage(Text.literal("Time log deleted.").formatted(Formatting.YELLOW));
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }

    private static int listTimeLogs(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String key = StringArgumentType.getString(context, "key");
        int page = getPage(context);

        var r = resolveTicket(source, key);
        if (r.isEmpty()) return 0;
        var project = r.get().project();
        var ticket  = r.get().ticket();

        int pageSize = 10;
        int offset = (page - 1) * pageSize;
        long totalLogs = com.mossman.MossManMod.getTimeLogRepository().countByTicketId(ticket.getId());
        long totalMinutes = com.mossman.MossManMod.getTimeLogRepository().sumMinutesByTicketId(ticket.getId());
        int totalPages = Math.max(1, (int) Math.ceil((double) totalLogs / pageSize));

        source.sendMessage(Text.literal("--- Time Logs: " + key + " (" + DurationParser.format(totalMinutes) + " total) ---").formatted(Formatting.AQUA));

        List<com.mossman.domain.entities.TimeLog> logs = com.mossman.MossManMod.getTimeLogRepository()
                .findByTicketId(ticket.getId(), offset, pageSize);

        if (logs.isEmpty()) {
            source.sendMessage(Text.literal("No time logged yet.").formatted(Formatting.GRAY));
        } else {
            ZoneId playerZone = TuiHelper.resolveZone(source);
            UUID currentPlayerId = source.getPlayer() != null ? source.getPlayer().getUuid() : null;
            boolean isEditor = com.mossman.domain.auth.PermissionChecker.hasPermission(project, source, com.mossman.domain.entities.Permission.EDITOR);
            for (var tlog : logs) {
                String dateStr = TuiHelper.formatTimestamp(tlog.loggedAt(), playerZone);
                boolean canDelete = isEditor || (currentPlayerId != null && currentPlayerId.equals(tlog.workerId()));
                MutableText logLine = Text.literal("  ").formatted(Formatting.WHITE);
                if (canDelete) {
                    logLine.append(TuiHelper.createRunLink("[✗]", "/mossman ticket log delete " + tlog.id(), "Delete time log", Formatting.RED));
                    logLine.append(Text.literal(" ").formatted(Formatting.WHITE));
                }
                logLine.append(Text.literal("[#" + tlog.id() + "] ").formatted(Formatting.GOLD));
                logLine.append(Text.literal(tlog.workerName() + " (" + dateStr + "): ").formatted(Formatting.GRAY));
                logLine.append(Text.literal(DurationParser.format(tlog.minutes())).formatted(Formatting.WHITE));
                if (!tlog.note().isBlank()) {
                    logLine.append(Text.literal(" — " + tlog.note()).formatted(Formatting.GRAY));
                }
                source.sendMessage(logLine);
            }
        }

        TuiHelper.sendPaginationFooter(source, page, totalPages, "/mossman ticket logs " + key);

        source.sendMessage(TuiHelper.createRunLink("[← Back to ticket]", "/mossman ticket view " + key, "View ticket", Formatting.GRAY));
        return 1;
    }

    private static int updateTicket(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String key = StringArgumentType.getString(context, "key");
        var nbt = NbtCompoundArgumentType.getNbtCompound(context, "patch");

        var r = resolveTicket(source, key);
        if (r.isEmpty()) return 0;
        var ticket = r.get().ticket();

        try {
            java.util.UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : java.util.UUID.randomUUID();
            var patch = NbtPatchParser.toMap(nbt);
            // Handle labels list separately (NbtPatchParser only handles string/numeric leaves)
            List<String> patchLabels = extractStringList(nbt, "labels");
            if (nbt.get("labels") != null) {
                patch.put("labels", patchLabels.stream().collect(Collectors.joining("|")));
            }
            com.mossman.MossManMod.getUpdateTicketUseCase().execute(ticket.getId(), rId, patch);

            source.sendMessage(Text.literal("Updated ticket " + key).formatted(Formatting.GREEN));
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }
    /**
     * Resolves a UUID to a display name.
     * Priority: project member list → online player → short UUID fallback.
     */
    private static String resolveUsername(UUID uuid, com.mossman.domain.entities.Project project, ServerCommandSource source) {
        var member = project.getMembers().stream()
                .filter(m -> m.uuid().equals(uuid))
                .findFirst();
        if (member.isPresent()) return member.get().username();
        var online = source.getServer().getPlayerManager().getPlayer(uuid);
        if (online != null) return online.getName().getString();
        return uuid.toString().substring(0, 8) + "...";
    }

    private static String getFilterNbt(CommandContext<ServerCommandSource> context) {
        try {
            return NbtCompoundArgumentType.getNbtCompound(context, "filter").toString();
        } catch (Exception e) {
            return "{}";
        }
    }
}
