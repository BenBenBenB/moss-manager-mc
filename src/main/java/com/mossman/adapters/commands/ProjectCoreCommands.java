package com.mossman.adapters.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.mossman.domain.auth.PermissionChecker;
import com.mossman.domain.entities.Permission;

import java.util.UUID;

class ProjectCoreCommands {

    static void register(LiteralArgumentBuilder<ServerCommandSource> projectNode) {
        projectNode
                .then(CommandManager.literal("list")
                        .executes(ProjectCoreCommands::listProjects)
                        .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                .executes(ProjectCoreCommands::listProjects)))
                .then(CommandManager.literal("view")
                        .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                .executes(ProjectCoreCommands::viewProject)))
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                        .executes(ProjectCoreCommands::createProject))))
                .then(CommandManager.literal("delete")
                        .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                .executes(ProjectCoreCommands::deleteProject)))
                .then(CommandManager.literal("update")
                        .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                .then(CommandManager.argument("patch", NbtCompoundArgumentType.nbtCompound())
                                        .suggests(ProjectSuggestions.suggestProjectPatch())
                                        .executes(ProjectCoreCommands::updateProject))));
    }

    private static int listProjects(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        source.sendMessage(TuiHelper.translatable("mossman.tui.project.list.header").formatted(Formatting.AQUA));

        int page = TuiHelper.getOptionalPage(context);

        int pageSize = 10;
        int offset = (page - 1) * pageSize;

        // Note: For projects, we filter in memory because of the complex permission check
        // In a real app, we'd want to do this in the DB
        var allProjects = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE);
        var visibleProjects = allProjects.stream()
                .filter(p -> PermissionChecker.canView(p, source))
                .toList();

        int totalVisible = visibleProjects.size();
        int totalPages = (int) Math.ceil((double) totalVisible / pageSize);

        var paginatedProjects = visibleProjects.stream()
                .skip(offset)
                .limit(pageSize)
                .toList();

        if (paginatedProjects.isEmpty()) {
            source.sendMessage(Text.literal("No projects available to view.").formatted(Formatting.GRAY));
        } else {
            for (var project : paginatedProjects) {
                String tc = project.getTextColor();
                MutableText projectLink = TuiHelper.applyColor(TuiHelper.createRunLink(
                        "[" + project.getTicketPrefix() + "]",
                        "/mossman project view " + project.getTicketPrefix(),
                        TuiHelper.translatable("mossman.tui.project.view_hover").getString(),
                        Formatting.GREEN), tc)
                        .append(TuiHelper.applyColor(Text.literal(" " + project.getName()).formatted(Formatting.WHITE), tc));
                source.sendMessage(projectLink);
            }
        }

        TuiHelper.sendPaginationFooter(source, page, totalPages, "/mossman project list");

        source.sendMessage(TuiHelper.translatable("mossman.tui.project.list.footer").formatted(Formatting.GRAY));

        MutableText createBtn = TuiHelper.createSuggestLink(
                TuiHelper.translatable("mossman.tui.project.create_btn").getString(),
                "/mossman project create ",
                TuiHelper.translatable("mossman.tui.project.create_hover").getString(),
                Formatting.GOLD
        );
        source.sendMessage(createBtn);

        return 1;
    }

    private static int viewProject(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");

        var projectOpt = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                .findFirst();

        if (projectOpt.isEmpty()) {
            source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED));
            return 0;
        }

        var project = projectOpt.get();
        if (!PermissionChecker.canView(project, source)) {
            source.sendMessage(Text.literal("You do not have permission to view this project.").formatted(Formatting.RED));
            return 0;
        }

        source.sendMessage(Text.literal("--- Project: [").formatted(Formatting.AQUA)
                .append(TuiHelper.applyColor(Text.literal(project.getTicketPrefix()), project.getTextColor()))
                .append(Text.literal("] ---").formatted(Formatting.AQUA)));

        boolean isEditor = PermissionChecker.hasPermission(project, source, Permission.EDITOR);

        // Ticket Prefix
        MutableText prefixLine = Text.empty();
        if (isEditor) {
            prefixLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project update " + project.getTicketPrefix() + " {ticketPrefix:\"" + project.getTicketPrefix() + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Ticket Prefix"), Formatting.GRAY));
        }
        prefixLine.append(Text.literal("Ticket Prefix: " + project.getTicketPrefix()).formatted(Formatting.WHITE));
        source.sendMessage(prefixLine);

        // Name
        MutableText nameLine = Text.empty();
        if (isEditor) {
            nameLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project update " + project.getTicketPrefix() + " {name:\"" + project.getName() + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Name"), Formatting.GRAY));
        }
        nameLine.append(Text.literal("Name: " + project.getName()).formatted(Formatting.WHITE));
        source.sendMessage(nameLine);

        // Description
        MutableText descLine = Text.empty();
        if (isEditor) {
            descLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project update " + project.getTicketPrefix() + " {description:\"" + (project.getDescription() != null ? project.getDescription() : "") + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Description"), Formatting.GRAY));
        }
        descLine.append(Text.literal("Description: ").formatted(Formatting.GRAY).append(Text.literal(project.getDescription() != null ? project.getDescription() : "None").formatted(Formatting.WHITE)));
        source.sendMessage(descLine);

        // Icon Texture
        MutableText iconLine = Text.empty();
        if (isEditor) {
            iconLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project update " + project.getTicketPrefix() + " {iconTexture:\"" + (project.getIconTexture() != null ? project.getIconTexture() : "") + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Icon Texture"), Formatting.GRAY));
        }
        iconLine.append(Text.literal("Icon Texture: " + (project.getIconTexture() != null ? project.getIconTexture() : "None")).formatted(Formatting.WHITE));
        source.sendMessage(iconLine);

        // Text Color
        MutableText colorLine = Text.empty();
        if (isEditor) {
            colorLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project update " + project.getTicketPrefix() + " {textColor:\"" + (project.getTextColor() != null ? project.getTextColor() : "") + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Text Color"), Formatting.GRAY));
        }
        colorLine.append(Text.literal("Text Color: " + (project.getTextColor() != null ? project.getTextColor() : "None")).formatted(Formatting.WHITE));
        source.sendMessage(colorLine);

        // External User Permission
        MutableText extPermLine = Text.empty();
        if (isEditor) {
            extPermLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project update " + project.getTicketPrefix() + " {externalUserPermission:\"" + project.getExternalUserPermission().name() + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "External User Permission"), Formatting.GRAY));
        }
        extPermLine.append(Text.literal("External Permission: " + project.getExternalUserPermission().name()).formatted(Formatting.WHITE));
        source.sendMessage(extPermLine);

        MutableText membersLink = TuiHelper.createRunLink(TuiHelper.translatable("mossman.tui.common.view_btn").getString(), "/mossman project member list " + project.getTicketPrefix(), "View project members", Formatting.GOLD);
        source.sendMessage(Text.literal("Members: ").formatted(Formatting.GRAY).append(Text.literal(project.getMembers().size() + " ").formatted(Formatting.WHITE)).append(membersLink));

        MutableText statusesLink = TuiHelper.createRunLink(TuiHelper.translatable("mossman.tui.common.view_btn").getString(), "/mossman project status list " + project.getTicketPrefix(), "View project statuses", Formatting.GOLD);
        source.sendMessage(Text.literal("Statuses: ").formatted(Formatting.GRAY).append(Text.literal(project.getStatuses().size() + " ").formatted(Formatting.WHITE)).append(statusesLink));

        MutableText ticketTypesLink = TuiHelper.createRunLink(TuiHelper.translatable("mossman.tui.common.view_btn").getString(), "/mossman project ticketType list " + project.getTicketPrefix(), "View project ticket types", Formatting.GOLD);
        source.sendMessage(Text.literal("Ticket Types: ").formatted(Formatting.GRAY).append(Text.literal(project.getTicketTypes().size() + " ").formatted(Formatting.WHITE)).append(ticketTypesLink));

        MutableText relTypesLink = TuiHelper.createRunLink(TuiHelper.translatable("mossman.tui.common.view_btn").getString(), "/mossman project relationshipType list " + project.getTicketPrefix(), "View project relationship types", Formatting.GOLD);
        source.sendMessage(Text.literal("Relationship Types: ").formatted(Formatting.GRAY).append(Text.literal(project.getRelationshipTypes().size() + " ").formatted(Formatting.WHITE)).append(relTypesLink));

        MutableText viewTicketsBtn = TuiHelper.createRunLink("[View Tickets] ", "/mossman ticket list " + project.getTicketPrefix(), "View tickets", Formatting.YELLOW);

        source.sendMessage(viewTicketsBtn);
        source.sendMessage(TuiHelper.translatable("mossman.tui.project.list.footer").formatted(Formatting.GRAY));

        return 1;
    }

    private static int createProject(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        String name = StringArgumentType.getString(context, "name");

        if (MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream().anyMatch(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))) {
            source.sendMessage(Text.literal("A project with prefix " + prefix + " already exists.").formatted(Formatting.RED));
            return 0;
        }

        try {
            var projectBuilder = com.mossman.domain.entities.Project.builder()
                    .ticketPrefix(prefix.toUpperCase())
                    .name(name);

            UUID creatorId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.nameUUIDFromBytes("mossman-system".getBytes());
            String creatorName = source.getPlayer() != null ? source.getPlayer().getName().getString() : "Server";

            String upperPrefix = prefix.toUpperCase();
            MossManMod.getCreateProjectUseCase().execute(projectBuilder, creatorId, creatorName);

            MutableText response = TuiHelper.translatable("mossman.tui.project.created", name).formatted(Formatting.GREEN);
            response.append(TuiHelper.createRunLink(
                    "[" + upperPrefix + "]",
                    "/mossman project view " + upperPrefix,
                    TuiHelper.translatable("mossman.tui.project.view_hover").getString(),
                    Formatting.GOLD
            ));
            source.sendMessage(response);
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }

    private static int deleteProject(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                .findFirst().orElse(null);

        if (project == null) {
            source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED));
            return 0;
        }

        try {
            if (!PermissionChecker.isOwner(project, source)) {
                source.sendMessage(Text.literal("Only the project owner can delete the project.").formatted(Formatting.RED));
                return 0;
            }
            MossManMod.getProjectRepository().delete(project.getId());
            source.sendMessage(Text.literal("Deleted Project: " + prefix).formatted(Formatting.RED));
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }

    private static int updateProject(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                .findFirst().orElse(null);

        if (project == null) {
            source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED));
            return 0;
        }

        try {
            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            var patch = NbtPatchParser.toMap(NbtCompoundArgumentType.getNbtCompound(context, "patch"));
            MossManMod.getUpdateProjectUseCase().execute(project.getId(), rId, patch);
            source.sendMessage(Text.literal("Updated project [" + project.getTicketPrefix() + "]").formatted(Formatting.GREEN));
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }
}
