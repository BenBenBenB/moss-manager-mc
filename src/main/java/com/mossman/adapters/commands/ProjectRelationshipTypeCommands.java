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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

class ProjectRelationshipTypeCommands {

    static void register(LiteralArgumentBuilder<ServerCommandSource> projectNode) {
        projectNode
                .then(CommandManager.literal("relationshipType")
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word())
                                                .executes(ProjectRelationshipTypeCommands::addRelationshipType))))
                        .then(CommandManager.literal("list")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .executes(ProjectRelationshipTypeCommands::listRelationshipTypes)))
                        .then(CommandManager.literal("view")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.greedyString()).suggests(ProjectSuggestions.suggestRelationshipTypeNames("prefix"))
                                                .executes(ProjectRelationshipTypeCommands::viewRelationshipType))))
                        .then(CommandManager.literal("update")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(ProjectSuggestions.suggestRelationshipTypeNames("prefix"))
                                                .then(CommandManager.argument("patch", NbtCompoundArgumentType.nbtCompound())
                                                        .suggests(ProjectSuggestions.suggestRelationshipTypePatch())
                                                        .executes(ProjectRelationshipTypeCommands::updateRelationshipType)))))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(ProjectSuggestions.suggestRelationshipTypeNames("prefix"))
                                                .executes(ProjectRelationshipTypeCommands::removeRelationshipType)
                                                .then(CommandManager.argument("replacement", StringArgumentType.word()).suggests(ProjectSuggestions.suggestRelationshipTypeNames("prefix"))
                                                        .executes(ProjectRelationshipTypeCommands::removeRelationshipType))))));
    }

    private static int listRelationshipTypes(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.canView(project, source)) return 0;

        source.sendMessage(Text.literal("--- Relationship Types of ").formatted(Formatting.AQUA)
                .append(TuiHelper.applyColor(Text.literal(project.getName()), project.getTextColor()))
                .append(Text.literal(" ---").formatted(Formatting.AQUA)));
        boolean isEditorRT = PermissionChecker.hasPermission(project, source, Permission.EDITOR);
        if (project.getRelationshipTypes().isEmpty()) {
            source.sendMessage(Text.literal("No relationship types found.").formatted(Formatting.GRAY));
        } else {
            for (var rt : project.getRelationshipTypes()) {
                MutableText line = Text.empty();
                if (isEditorRT) {
                    line.append(TuiHelper.createRunLink("[View] ", "/mossman project relationshipType view " + prefix + " " + rt.key(), "View " + rt.key(), Formatting.GOLD));
                    line.append(TuiHelper.createSuggestLink("[✗] ", "/mossman project relationshipType remove " + prefix + " " + rt.key(), "Remove " + rt.key(), Formatting.RED));
                }
                line.append(coloredName(rt.displayName(), rt.textColor()));
                source.sendMessage(line);
            }
        }

        if (isEditorRT) {
            source.sendMessage(TuiHelper.createSuggestLink("[Add Relationship Type] ", "/mossman project relationshipType add " + prefix + " ", "Click to add a new relationship type", Formatting.GOLD));
        }
        return 1;
    }

    private static int viewRelationshipType(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.canView(project, source)) return 0;
        var rt = project.getRelationshipTypes().stream().filter(t -> t.key().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (rt == null) return 0;

        source.sendMessage(Text.literal("--- Relationship Type: " + rt.key() + " ---").formatted(Formatting.AQUA));
        boolean isEditor = PermissionChecker.hasPermission(project, source, Permission.EDITOR);

        MutableText keyLine = Text.empty();
        if (isEditor) keyLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project relationshipType update " + prefix + " " + rt.key() + " {key:\"" + rt.key() + "\"}", "Click to edit Key", Formatting.GRAY));
        source.sendMessage(keyLine.append(Text.literal("Key: " + rt.key()).formatted(Formatting.WHITE)));

        MutableText displayNameLine = Text.empty();
        if (isEditor) displayNameLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project relationshipType update " + prefix + " " + rt.key() + " {displayName:\"" + rt.displayName() + "\"}", "Click to edit Display Name", Formatting.GRAY));
        source.sendMessage(displayNameLine.append(Text.literal("Display Name: " + rt.displayName()).formatted(Formatting.WHITE)));

        MutableText sourceToTargetLine = Text.empty();
        if (isEditor) sourceToTargetLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project relationshipType update " + prefix + " " + rt.key() + " {sourceToTargetDescription:\"" + (rt.sourceToTargetDescription() != null ? rt.sourceToTargetDescription() : "") + "\"}", "Click to edit Source -> Target", Formatting.GRAY));
        source.sendMessage(sourceToTargetLine.append(Text.literal("Source->Target: " + (rt.sourceToTargetDescription() != null ? rt.sourceToTargetDescription() : "None")).formatted(Formatting.WHITE)));

        MutableText targetToSourceLine = Text.empty();
        if (isEditor) targetToSourceLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project relationshipType update " + prefix + " " + rt.key() + " {targetToSourceDescription:\"" + (rt.targetToSourceDescription() != null ? rt.targetToSourceDescription() : "") + "\"}", "Click to edit Target -> Source", Formatting.GRAY));
        source.sendMessage(targetToSourceLine.append(Text.literal("Target->Source: " + (rt.targetToSourceDescription() != null ? rt.targetToSourceDescription() : "None")).formatted(Formatting.WHITE)));

        MutableText colorLine = Text.empty();
        if (isEditor) colorLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project relationshipType update " + prefix + " " + rt.key() + " {textColor:\"" + (rt.textColor() != null ? rt.textColor() : "") + "\"}", "Click to edit Text Color", Formatting.GRAY));
        source.sendMessage(colorLine.append(Text.literal("Text Color: " + (rt.textColor() != null ? rt.textColor() : "None")).formatted(Formatting.WHITE)));
        return 1;
    }

    private static int updateRelationshipType(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.hasPermission(project, source, Permission.EDITOR)) return 0;

        var patch = NbtPatchParser.toMap(NbtCompoundArgumentType.getNbtCompound(context, "patch"));
        var target = project.getRelationshipTypes().stream().filter(t -> t.key().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (target == null) return 0;

        List<com.mossman.domain.entities.RelationshipType> newLists = new ArrayList<>(project.getRelationshipTypes());
        newLists.remove(target);
        newLists.add(new com.mossman.domain.entities.RelationshipType(
            patch.getOrDefault("key", target.key()),
            patch.getOrDefault("displayName", target.displayName()),
            patch.getOrDefault("sourceToTargetDescription", target.sourceToTargetDescription()),
            patch.getOrDefault("targetToSourceDescription", target.targetToSourceDescription()),
            patch.getOrDefault("textColor", target.textColor())
        ));

        try {
            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            MossManMod.getUpdateProjectRelationshipTypesUseCase().execute(project.getId(), rId, newLists);
            source.sendMessage(Text.literal("Updated relationship type").formatted(Formatting.GREEN));
        } catch (IllegalArgumentException e) {
            source.sendMessage(Text.literal(e.getMessage()).formatted(Formatting.RED));
        }
        return 1;
    }

    private static int addRelationshipType(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.hasPermission(project, source, Permission.EDITOR)) return 0;

        if (project.getRelationshipTypes().stream().anyMatch(t -> t.key().equalsIgnoreCase(name))) {
            source.sendMessage(Text.literal("Relationship type already exists.").formatted(Formatting.RED));
            return 0;
        }

        List<com.mossman.domain.entities.RelationshipType> newLists = new ArrayList<>(project.getRelationshipTypes());
        newLists.add(new com.mossman.domain.entities.RelationshipType(name, name, "", "", ""));

        try {
            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            MossManMod.getUpdateProjectRelationshipTypesUseCase().execute(project.getId(), rId, newLists);
            source.sendMessage(Text.literal("Added relationship type " + name).formatted(Formatting.GREEN));
        } catch (IllegalArgumentException e) {
            source.sendMessage(Text.literal(e.getMessage()).formatted(Formatting.RED));
        }
        return 1;
    }

    private static int removeRelationshipType(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        String replacementName = null;
        try { replacementName = StringArgumentType.getString(context, "replacement"); } catch (Exception ignored) {}

        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.hasPermission(project, source, Permission.EDITOR)) return 0;

        var target = project.getRelationshipTypes().stream().filter(t -> t.key().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (target == null) { source.sendMessage(Text.literal("Relationship type not found: " + name).formatted(Formatting.RED)); return 0; }

        List<com.mossman.domain.entities.RelationshipType> newLists = new ArrayList<>(project.getRelationshipTypes());
        newLists.remove(target);

        // Find all existing relationships of this type across all project tickets
        var allTickets = MossManMod.getTicketRepository().findByProjectId(project.getId(), 0, Integer.MAX_VALUE);
        var relRepo = MossManMod.getTicketRelationshipRepository();
        Set<Long> seen = new HashSet<>();
        List<com.mossman.domain.entities.TicketRelationship> affectedRels = new ArrayList<>();
        for (var ticket : allTickets) {
            for (var rel : relRepo.findByTicketId(ticket.getId())) {
                if (rel.type().equalsIgnoreCase(target.key()) && seen.add(rel.id())) {
                    affectedRels.add(rel);
                }
            }
        }

        final String finalReplacementName = replacementName;
        com.mossman.domain.entities.RelationshipType replacementType = null;
        if (finalReplacementName != null) {
            replacementType = newLists.stream().filter(t -> t.key().equalsIgnoreCase(finalReplacementName)).findFirst().orElse(null);
            if (replacementType == null) {
                source.sendMessage(Text.literal("Replacement relationship type not found: " + replacementName).formatted(Formatting.RED));
                return 0;
            }
        }

        try {
            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            if (replacementType != null) {
                for (var rel : affectedRels) {
                    relRepo.save(new com.mossman.domain.entities.TicketRelationship(rel.id(), replacementType.key(), rel.sourceTicketId(), rel.targetTicketId()));
                }
            } else {
                for (var rel : affectedRels) {
                    relRepo.delete(rel.id());
                }
            }
            MossManMod.getUpdateProjectRelationshipTypesUseCase().execute(project.getId(), rId, newLists);
            String msg = "Removed relationship type " + name;
            if (!affectedRels.isEmpty()) {
                msg += replacementType != null
                        ? "; migrated " + affectedRels.size() + " relationship(s) to " + replacementType.key()
                        : "; deleted " + affectedRels.size() + " relationship(s)";
            }
            source.sendMessage(Text.literal(msg).formatted(Formatting.YELLOW));
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
