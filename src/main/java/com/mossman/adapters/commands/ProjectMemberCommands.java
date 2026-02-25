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
import com.mojang.authlib.GameProfile;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.mossman.domain.auth.PermissionChecker;
import com.mossman.domain.entities.Permission;

import java.util.UUID;
import java.util.Collection;

class ProjectMemberCommands {

    static void register(LiteralArgumentBuilder<ServerCommandSource> projectNode) {
        projectNode
                .then(CommandManager.literal("member")
                        .then(CommandManager.literal("list")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .executes(ProjectMemberCommands::listMembers)
                                        .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                                .executes(ProjectMemberCommands::listMembers))))
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                                .then(CommandManager.argument("permission", StringArgumentType.word()).suggests(SuggestionHelper::suggestAssignablePermissions)
                                                        .executes(ProjectMemberCommands::addMember)))))
                        .then(CommandManager.literal("view")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                                .executes(ProjectMemberCommands::viewMember))))
                        .then(CommandManager.literal("update")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                                .then(CommandManager.argument("patch", NbtCompoundArgumentType.nbtCompound())
                                                        .suggests(ProjectSuggestions.suggestMemberPatch())
                                                        .executes(ProjectMemberCommands::updateMember)))))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                                .executes(ProjectMemberCommands::removeMember))))
                        .then(CommandManager.literal("transfer")
                                .then(CommandManager.argument("prefix", StringArgumentType.word()).suggests(SuggestionHelper::suggestVisiblePrefixes)
                                        .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                                .executes(ProjectMemberCommands::transferOwnership)))));
    }

    private static int listMembers(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                .findFirst().orElse(null);

        if (project == null) {
            source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED));
            return 0;
        }

        if (!PermissionChecker.canView(project, source)) {
            source.sendMessage(Text.literal("You do not have permission to view members.").formatted(Formatting.RED));
            return 0;
        }

        int page = TuiHelper.getOptionalPage(context);

        int pageSize = 10;
        int offset = (page - 1) * pageSize;

        var members = MossManMod.getMemberRepository().findByProjectId(project.getId(), offset, pageSize);
        long totalMembers = MossManMod.getMemberRepository().countByProjectId(project.getId());
        int totalPages = (int) Math.ceil((double) totalMembers / pageSize);

        source.sendMessage(Text.literal("--- Members of ").formatted(Formatting.AQUA)
                .append(TuiHelper.applyColor(Text.literal(project.getName()), project.getTextColor()))
                .append(Text.literal(" ---").formatted(Formatting.AQUA)));
        if (members.isEmpty()) {
            source.sendMessage(Text.literal("No members found.").formatted(Formatting.GRAY));
        } else {
            for (var member : members) {
                MutableText mText = TuiHelper.createRunLink("[" + member.username() + "] ", "/mossman project member view " + prefix + " " + member.username(), "View member details", Formatting.GREEN)
                        .append(Text.literal(member.username() + " (").formatted(Formatting.WHITE))
                        .append(Text.literal(member.permission().name()).formatted(Formatting.YELLOW))
                        .append(Text.literal(") ").formatted(Formatting.WHITE));
                if (member.title() != null && !member.title().isEmpty()) {
                    mText.append(Text.literal(member.title()).formatted(Formatting.GRAY));
                }
                source.sendMessage(mText);
            }
        }

        if (PermissionChecker.isOwner(project, source)) {
            source.sendMessage(TuiHelper.createSuggestLink("[Add Member] ", "/mossman project member add " + prefix + " ", "Click to add a new member", Formatting.GOLD));
        }

        TuiHelper.sendPaginationFooter(source, page, totalPages, "/mossman project member list " + prefix);

        return 1;
    }

    private static int viewMember(CommandContext<ServerCommandSource> context) {
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
            source.sendMessage(Text.literal("You do not have permission to view members.").formatted(Formatting.RED));
            return 0;
        }

        boolean isEditor = PermissionChecker.hasPermission(project, source, Permission.EDITOR);

        try {
            var targetProfiles = GameProfileArgumentType.getProfileArgument(context, "player");

            for (var profile : targetProfiles) {
                var memberOpt = MossManMod.getMemberRepository().findByProjectIdAndUuid(project.getId(), profile.id());
                if (memberOpt.isEmpty()) {
                    source.sendMessage(Text.literal("Member not found in project: " + profile.name()).formatted(Formatting.RED));
                    continue;
                }
                var member = memberOpt.get();

                source.sendMessage(Text.literal("--- Member: " + member.username() + " ---").formatted(Formatting.AQUA));

                // Title
                MutableText titleLine = Text.empty();
                if (isEditor) {
                    titleLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project member update " + prefix + " " + member.username() + " {title:\"" + (member.title() != null ? member.title() : "") + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Title"), Formatting.GRAY));
                }
                titleLine.append(Text.literal("Title: " + (member.title() != null ? member.title() : "None")).formatted(Formatting.WHITE));
                source.sendMessage(titleLine);

                // Permission
                MutableText permLine = Text.empty();
                if (isEditor && PermissionChecker.isOwner(project, source)) {
                    permLine.append(TuiHelper.createSuggestLink("[✎] ", "/mossman project member update " + prefix + " " + member.username() + " {permission:\"" + member.permission().name() + "\"}", TuiHelper.translatable("mossman.tui.ticket.edit_field_hover", "Permission"), Formatting.GRAY));
                }
                permLine.append(Text.literal("Permission: " + member.permission().name()).formatted(Formatting.YELLOW));
                source.sendMessage(permLine);
            }
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }

    private static int addMember(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        try {
            String prefix = StringArgumentType.getString(context, "prefix");
            var targetProfiles = GameProfileArgumentType.getProfileArgument(context, "player");
            Permission perm = Permission.valueOf(StringArgumentType.getString(context, "permission").toUpperCase());
            var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                    .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Project not found"));

            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            for (var entry : targetProfiles) {
                var member = new com.mossman.domain.entities.Member(0, entry.id(), entry.name(), "", perm);
                MossManMod.getAddMemberUseCase().execute(project.getId(), rId, member);
                source.sendMessage(Text.literal("Added " + entry.name() + " as " + perm).formatted(Formatting.GREEN));
            }
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }

    private static int updateMember(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        try {
            String prefix = StringArgumentType.getString(context, "prefix");
            var targetProfiles = GameProfileArgumentType.getProfileArgument(context, "player");
            var patch = NbtPatchParser.toMap(NbtCompoundArgumentType.getNbtCompound(context, "patch"));
            var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                    .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Project not found"));

            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            for (var entry : targetProfiles) {
                MossManMod.getUpdateMemberUseCase().execute(project.getId(), rId, entry.id(), patch);
                source.sendMessage(Text.literal("Updated member " + entry.name()).formatted(Formatting.GREEN));
            }
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }

    private static int removeMember(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        try {
            String prefix = StringArgumentType.getString(context, "prefix");
            var targetProfiles = GameProfileArgumentType.getProfileArgument(context, "player");
            var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                    .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Project not found"));

            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            for (var entry : targetProfiles) {
                MossManMod.getRemoveMemberUseCase().execute(project.getId(), rId, entry.id());
                source.sendMessage(Text.literal("Removed member " + entry.name()).formatted(Formatting.YELLOW));
            }
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }

    private static int transferOwnership(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        try {
            String prefix = StringArgumentType.getString(context, "prefix");
            var entry = GameProfileArgumentType.getProfileArgument(context, "player").iterator().next();
            var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                    .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Project not found"));

            UUID rId = source.getPlayer() != null ? source.getPlayer().getUuid() : UUID.randomUUID();
            MossManMod.getTransferOwnershipUseCase().execute(project.getId(), rId, entry.id());
            source.sendMessage(Text.literal("Transferred ownership of " + project.getTicketPrefix() + " to " + entry.name()).formatted(Formatting.GOLD));
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }
}
