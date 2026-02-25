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

class ProjectStatusCommands {

    static void register(LiteralArgumentBuilder<ServerCommandSource> projectNode) {
        projectNode
                .then(CommandManager.literal("status")
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word())
                                                .executes(ProjectStatusCommands::addStatus))))
                        .then(CommandManager.literal("list")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .executes(ProjectStatusCommands::listStatuses)))
                        .then(CommandManager.literal("view")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(ProjectSuggestions.suggestStatusNames("prefix"))
                                                .executes(ProjectStatusCommands::viewStatus))))
                        .then(CommandManager.literal("update")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(ProjectSuggestions.suggestStatusNames("prefix"))
                                                .then(CommandManager.argument("patch", NbtCompoundArgumentType.nbtCompound())
                                                        .suggests(ProjectSuggestions.suggestStatusPatch())
                                                        .executes(ProjectStatusCommands::updateStatus)))))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("name", StringArgumentType.word()).suggests(ProjectSuggestions.suggestStatusNames("prefix"))
                                                .then(CommandManager.argument("replacement", StringArgumentType.word()).suggests(ProjectSuggestions.suggestStatusNames("prefix"))
                                                        .executes(ProjectStatusCommands::removeStatus))))));
    }

    private static int listStatuses(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null) { source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED)); return 0; }
        if (!PermissionChecker.canView(project, source)) { source.sendMessage(Text.literal("No permission.").formatted(Formatting.RED)); return 0; }

        source.sendMessage(Text.literal("--- Statuses of ").formatted(Formatting.AQUA)
                .append(TuiHelper.applyColor(Text.literal(project.getName()), project.getTextColor()))
                .append(Text.literal(" ---").formatted(Formatting.AQUA)));
        boolean isEditorStatus = PermissionChecker.hasPermission(project, source, Permission.EDITOR);
        if (project.getStatuses().isEmpty()) {
            source.sendMessage(Text.literal("No statuses found.").formatted(Formatting.GRAY));
        } else {
            for (var status : project.getStatuses()) {
                MutableText line = Text.empty();
                if (isEditorStatus) {
                    line.append(TuiHelper.createRunLink("[View] ", "/mossman project status view " + prefix + " " + status.key(), "View " + status.key(), Formatting.GOLD));
                    line.append(TuiHelper.createSuggestLink("[✗] ", "/mossman project status remove " + prefix + " " + status.key() + " ", "Remove " + status.key() + " (type replacement)", Formatting.RED));
                }
                line.append(coloredName(status.displayName(), status.textColor()));
                source.sendMessage(line);
            }
        }

        if (isEditorStatus) {
            source.sendMessage(TuiHelper.createSuggestLink("[Add Status] ", "/mossman project status add " + prefix + " ", "Click to add a new status", Formatting.GOLD));
        }
        return 1;
    }

    private static int viewStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.canView(project, source)) return 0;
        var status = project.getStatuses().stream().filter(s -> s.key().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (status == null) { source.sendMessage(Text.literal("Status not found: " + name).formatted(Formatting.RED)); return 0; }

        source.sendMessage(Text.literal("--- Status: " + status.key() + " ---").formatted(Formatting.AQUA));
        boolean isEditor = PermissionChecker.hasPermission(project, source, Permission.EDITOR);

        MutableText keyLine = Text.empty();
        if (isEditor) keyLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project status update " + prefix + " " + status.key() + " {key:\"" + status.key() + "\"}", "Click to edit Key", Formatting.GRAY));
        source.sendMessage(keyLine.append(Text.literal("Key: " + status.key()).formatted(Formatting.WHITE)));

        MutableText displayNameLine = Text.empty();
        if (isEditor) displayNameLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project status update " + prefix + " " + status.key() + " {displayName:\"" + status.displayName() + "\"}", "Click to edit Display Name", Formatting.GRAY));
        source.sendMessage(displayNameLine.append(Text.literal("Display Name: " + status.displayName()).formatted(Formatting.WHITE)));

        MutableText colorLine = Text.empty();
        if (isEditor) colorLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project status update " + prefix + " " + status.key() + " {textColor:\"" + (status.textColor() != null ? status.textColor() : "") + "\"}", "Click to edit Text Color", Formatting.GRAY));
        source.sendMessage(colorLine.append(Text.literal("Text Color: " + (status.textColor() != null ? status.textColor() : "None")).formatted(Formatting.WHITE)));

        return 1;
    }

    private static int updateStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.hasPermission(project, source, Permission.EDITOR)) return 0;

        var patch = NbtPatchParser.toMap(NbtCompoundArgumentType.getNbtCompound(context, "patch"));
        var target = project.getStatuses().stream().filter(s -> s.key().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (target == null) return 0;

        List<com.mossman.domain.entities.Status> newStatuses = new ArrayList<>(project.getStatuses());
        newStatuses.remove(target);
        newStatuses.add(new com.mossman.domain.entities.Status(
            patch.getOrDefault("key", target.key()),
            patch.getOrDefault("displayName", target.displayName()),
            patch.getOrDefault("textColor", target.textColor())
        ));

        try {
            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            MossManMod.getUpdateProjectStatusesUseCase().execute(project.getId(), rId, newStatuses);
            source.sendMessage(Text.literal("Updated status").formatted(Formatting.GREEN));
        } catch (IllegalArgumentException e) {
            source.sendMessage(Text.literal(e.getMessage()).formatted(Formatting.RED));
        }
        return 1;
    }

    private static int addStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.hasPermission(project, source, Permission.EDITOR)) return 0;

        if (project.getStatuses().stream().anyMatch(s -> s.key().equalsIgnoreCase(name))) {
            source.sendMessage(Text.literal("Status already exists.").formatted(Formatting.RED));
            return 0;
        }

        List<com.mossman.domain.entities.Status> newStatuses = new ArrayList<>(project.getStatuses());
        newStatuses.add(new com.mossman.domain.entities.Status(name, name, ""));

        try {
            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            MossManMod.getUpdateProjectStatusesUseCase().execute(project.getId(), rId, newStatuses);
            source.sendMessage(Text.literal("Added status " + name).formatted(Formatting.GREEN));
        } catch (IllegalArgumentException e) {
            source.sendMessage(Text.literal(e.getMessage()).formatted(Formatting.RED));
        }
        return 1;
    }

    private static int removeStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");
        String replacementName = StringArgumentType.getString(context, "replacement");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null || !PermissionChecker.hasPermission(project, source, Permission.EDITOR)) return 0;

        var target = project.getStatuses().stream().filter(s -> s.key().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (target == null) { source.sendMessage(Text.literal("Status not found: " + name).formatted(Formatting.RED)); return 0; }

        List<com.mossman.domain.entities.Status> newStatuses = new ArrayList<>(project.getStatuses());
        newStatuses.remove(target);

        var replacementStatus = newStatuses.stream().filter(s -> s.key().equalsIgnoreCase(replacementName)).findFirst().orElse(null);
        if (replacementStatus == null) {
            source.sendMessage(Text.literal("Replacement status not found: " + replacementName).formatted(Formatting.RED));
            return 0;
        }

        try {
            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            MossManMod.getUpdateProjectStatusesUseCase().execute(project.getId(), rId, newStatuses);

            int migrated = 0;
            for (var ticket : MossManMod.getTicketRepository().findByProjectId(project.getId(), 0, Integer.MAX_VALUE)) {
                if (ticket.getStatus().equalsIgnoreCase(name)) {
                    MossManMod.getTicketRepository().save(new com.mossman.domain.entities.Ticket(
                            ticket.getId(), ticket.getProjectId(), ticket.getTicketNumber(),
                            ticket.getTitle(), ticket.getDescription(), ticket.getType(),
                            replacementStatus.key(), ticket.getPriority(),
                            ticket.getAssignees(), ticket.getObservers(), ticket.getCreator(),
                            ticket.getLabels(), ticket.getCreatedAt(), System.currentTimeMillis(), ticket.getSprintId()
                    ));
                    migrated++;
                }
            }

            source.sendMessage(Text.literal("Removed status " + name + "; migrated " + migrated + " ticket(s) to " + replacementStatus.key()).formatted(Formatting.YELLOW));
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
