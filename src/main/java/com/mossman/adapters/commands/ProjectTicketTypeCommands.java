package com.mossman.adapters.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mossman.adapters.tui.NbtPatchParser;
import com.mossman.adapters.tui.ProjectSuggestions;
import com.mossman.adapters.tui.SuggestionHelper;
import com.mossman.adapters.tui.TuiHelper;
import com.mossman.MossManMod;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.mossman.domain.auth.PermissionChecker;
import com.mossman.domain.entities.Permission;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class ProjectTicketTypeCommands {

    static void register(LiteralArgumentBuilder<ServerCommandSource> projectNode) {
        projectNode
                .then(CommandManager.literal("ticketType")
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word())
                                                .executes(ProjectTicketTypeCommands::addTicketType))))
                        .then(CommandManager.literal("list")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .executes(ProjectTicketTypeCommands::listTicketTypes)))
                        .then(CommandManager.literal("view")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(ProjectSuggestions.suggestTicketTypeNames("prefix"))
                                                .executes(ProjectTicketTypeCommands::viewTicketType))))
                        .then(CommandManager.literal("update")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(ProjectSuggestions.suggestTicketTypeNames("prefix"))
                                                .then(CommandManager.argument("patch", NbtCompoundArgumentType.nbtCompound())
                                                        .suggests(ProjectSuggestions.suggestTicketTypePatch())
                                                        .executes(ProjectTicketTypeCommands::updateTicketType)))))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(ProjectSuggestions.suggestTicketTypeNames("prefix"))
                                                .then(CommandManager.argument("replacement", StringArgumentType.word()).suggests(ProjectSuggestions.suggestTicketTypeNames("prefix"))
                                                        .executes(ProjectTicketTypeCommands::removeTicketType))))));
    }

    private static int listTicketTypes(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.canView(project, source)) return 0;

        source.sendMessage(Text.literal("--- Ticket Types of ").formatted(Formatting.AQUA)
                .append(TuiHelper.applyColor(Text.literal(project.getName()), project.getTextColor()))
                .append(Text.literal(" ---").formatted(Formatting.AQUA)));
        boolean isEditorTT = PermissionChecker.hasPermission(project, source, Permission.EDITOR);
        if (project.getTicketTypes().isEmpty()) {
            source.sendMessage(Text.literal("No ticket types found.").formatted(Formatting.GRAY));
        } else {
            for (var tt : project.getTicketTypes()) {
                MutableText line = Text.empty();
                if (isEditorTT) {
                    line.append(TuiHelper.createRunLink("[View] ", "/mossman project ticketType view " + prefix + " " + tt.key(), "View " + tt.key(), Formatting.GOLD));
                    line.append(TuiHelper.createSuggestLink("[✗] ", "/mossman project ticketType remove " + prefix + " " + tt.key() + " ", "Remove " + tt.key() + " (type replacement)", Formatting.RED));
                }
                line.append(coloredName(tt.displayName(), tt.textColor()));
                source.sendMessage(line);
            }
        }

        if (isEditorTT) {
            source.sendMessage(TuiHelper.createSuggestLink("[Add Ticket Type] ", "/mossman project ticketType add " + prefix + " ", "Click to add a new ticket type", Formatting.GOLD));
        }
        return 1;
    }

    private static int viewTicketType(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.canView(project, source)) return 0;
        var tt = project.getTicketTypes().stream().filter(t -> t.key().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (tt == null) return 0;

        source.sendMessage(Text.literal("--- Ticket Type: " + tt.key() + " ---").formatted(Formatting.AQUA));
        boolean isEditor = PermissionChecker.hasPermission(project, source, Permission.EDITOR);

        MutableText keyLine = Text.empty();
        if (isEditor) keyLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project ticketType update " + prefix + " " + tt.key() + " {key:\"" + tt.key() + "\"}", "Click to edit Key", Formatting.GRAY));
        source.sendMessage(keyLine.append(Text.literal("Key: " + tt.key()).formatted(Formatting.WHITE)));

        MutableText displayNameLine = Text.empty();
        if (isEditor) displayNameLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project ticketType update " + prefix + " " + tt.key() + " {displayName:\"" + tt.displayName() + "\"}", "Click to edit Display Name", Formatting.GRAY));
        source.sendMessage(displayNameLine.append(Text.literal("Display Name: " + tt.displayName()).formatted(Formatting.WHITE)));

        MutableText colorLine = Text.empty();
        if (isEditor) colorLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project ticketType update " + prefix + " " + tt.key() + " {textColor:\"" + (tt.textColor() != null ? tt.textColor() : "") + "\"}", "Click to edit Text Color", Formatting.GRAY));
        source.sendMessage(colorLine.append(Text.literal("Text Color: " + (tt.textColor() != null ? tt.textColor() : "None")).formatted(Formatting.WHITE)));
        return 1;
    }

    private static int updateTicketType(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.hasPermission(project, source, Permission.EDITOR)) return 0;

        var patch = NbtPatchParser.toMap(NbtCompoundArgumentType.getNbtCompound(context, "patch"));
        var target = project.getTicketTypes().stream().filter(t -> t.key().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (target == null) return 0;

        List<com.mossman.domain.entities.TicketType> newLists = new ArrayList<>(project.getTicketTypes());
        newLists.remove(target);
        newLists.add(new com.mossman.domain.entities.TicketType(
            patch.getOrDefault("key", target.key()),
            patch.getOrDefault("displayName", target.displayName()),
            patch.getOrDefault("textColor", target.textColor())
        ));

        try {
            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            MossManMod.getUpdateProjectTicketTypesUseCase().execute(project.getId(), rId, newLists);
            source.sendMessage(Text.literal("Updated ticket type").formatted(Formatting.GREEN));
        } catch (IllegalArgumentException e) {
            source.sendMessage(Text.literal(e.getMessage()).formatted(Formatting.RED));
        }
        return 1;
    }

    private static int addTicketType(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.hasPermission(project, source, Permission.EDITOR)) return 0;

        if (project.getTicketTypes().stream().anyMatch(t -> t.key().equalsIgnoreCase(name))) {
            source.sendMessage(Text.literal("Ticket type already exists.").formatted(Formatting.RED));
            return 0;
        }

        List<com.mossman.domain.entities.TicketType> newLists = new ArrayList<>(project.getTicketTypes());
        newLists.add(new com.mossman.domain.entities.TicketType(name, name, ""));

        try {
            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            MossManMod.getUpdateProjectTicketTypesUseCase().execute(project.getId(), rId, newLists);
            source.sendMessage(Text.literal("Added ticket type " + name).formatted(Formatting.GREEN));
        } catch (IllegalArgumentException e) {
            source.sendMessage(Text.literal(e.getMessage()).formatted(Formatting.RED));
        }
        return 1;
    }

    private static int removeTicketType(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        String replacementName = StringArgumentType.getString(context, "replacement");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.hasPermission(project, source, Permission.EDITOR)) return 0;

        var target = project.getTicketTypes().stream().filter(t -> t.key().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (target == null) { source.sendMessage(Text.literal("Ticket type not found: " + name).formatted(Formatting.RED)); return 0; }

        List<com.mossman.domain.entities.TicketType> newLists = new ArrayList<>(project.getTicketTypes());
        newLists.remove(target);

        var replacementType = newLists.stream().filter(t -> t.key().equalsIgnoreCase(replacementName)).findFirst().orElse(null);
        if (replacementType == null) {
            source.sendMessage(Text.literal("Replacement ticket type not found: " + replacementName).formatted(Formatting.RED));
            return 0;
        }

        try {
            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            MossManMod.getUpdateProjectTicketTypesUseCase().execute(project.getId(), rId, newLists);

            int migrated = 0;
            for (var ticket : MossManMod.getTicketRepository().findByProjectId(project.getId(), 0, Integer.MAX_VALUE)) {
                if (ticket.getType().equalsIgnoreCase(name)) {
                    MossManMod.getTicketRepository().save(new com.mossman.domain.entities.Ticket(
                            ticket.getId(), ticket.getProjectId(), ticket.getTicketNumber(),
                            ticket.getTitle(), ticket.getDescription(), replacementType.key(),
                            ticket.getStatus(), ticket.getPriority(),
                            ticket.getAssignees(), ticket.getObservers(), ticket.getCreator(),
                            ticket.getLabels(), ticket.getCreatedAt(), System.currentTimeMillis(), ticket.getSprintId()
                    ));
                    migrated++;
                }
            }

            source.sendMessage(Text.literal("Removed ticket type " + name + "; migrated " + migrated + " ticket(s) to " + replacementType.key()).formatted(Formatting.YELLOW));
        } catch (IllegalArgumentException e) {
            source.sendMessage(Text.literal(e.getMessage()).formatted(Formatting.RED));
        }
        return 1;
    }

    private static MutableText coloredName(String name, String textColor) {
        if (textColor != null && !textColor.isBlank()) {
            Formatting fmt = Formatting.byName(textColor.toLowerCase());
            if (fmt != null && fmt.isColor()) return Text.literal(name).formatted(fmt);
            if (textColor.startsWith("#") && textColor.length() == 7) {
                try {
                    int rgb = Integer.parseInt(textColor.substring(1), 16);
                    return Text.literal(name).setStyle(Style.EMPTY.withColor(rgb));
                } catch (NumberFormatException ignored) {}
            }
        }
        return Text.literal(name).formatted(Formatting.WHITE);
    }
}
