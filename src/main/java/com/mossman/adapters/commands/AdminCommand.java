package com.mossman.adapters.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mossman.MossManMod;
import com.mossman.adapters.tui.SuggestionHelper;
import com.mossman.adapters.tui.TuiHelper;
import com.mossman.domain.entities.Member;
import com.mossman.domain.entities.Permission;
import com.mossman.domain.entities.Project;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralCommandNode<ServerCommandSource> rootNode) {
        var adminNode = CommandManager.literal("admin")
                .then(CommandManager.literal("wipe")
                        .executes(AdminCommand::wipeDatabase))
                .then(CommandManager.literal("setProjectOwner")
                        .then(CommandManager.argument("prefix", StringArgumentType.word())
                                .suggests(SuggestionHelper::suggestVisiblePrefixes)
                                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                        .executes(AdminCommand::setProjectOwner))))
                .build();

        rootNode.addChild(adminNode);
    }

    private static int setProjectOwner(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String prefix = StringArgumentType.getString(context, "prefix");
        var project = MossManMod.getProjectRepository().findAll(0, Integer.MAX_VALUE).stream()
                .filter(p -> p.getTicketPrefix().equalsIgnoreCase(prefix)).findFirst().orElse(null);
        if (project == null) {
            source.sendMessage(Text.literal("Project not found: " + prefix).formatted(Formatting.RED));
            return 0;
        }
        try {
            for (var profile : GameProfileArgumentType.getProfileArgument(context, "player")) {
                List<Member> members = new ArrayList<>(project.getMembers());
                boolean targetIsMember = members.stream().anyMatch(m -> m.uuid().equals(profile.id()));
                if (!targetIsMember) {
                    members.add(new Member(0, profile.id(), profile.name(), "", Permission.OWNER));
                }
                List<Member> updatedMembers = members.stream().map(m -> {
                    if (m.uuid().equals(profile.id())) return new Member(m.id(), m.uuid(), m.username(), m.title(), Permission.OWNER);
                    if (m.permission() == Permission.OWNER) return new Member(m.id(), m.uuid(), m.username(), m.title(), Permission.ADMIN);
                    return m;
                }).collect(Collectors.toList());
                Project updated = Project.builder()
                        .id(project.getId())
                        .ticketPrefix(project.getTicketPrefix())
                        .name(project.getName())
                        .description(project.getDescription())
                        .iconTexture(project.getIconTexture())
                        .statuses(project.getStatuses())
                        .ticketTypes(project.getTicketTypes())
                        .relationshipTypes(project.getRelationshipTypes())
                        .members(updatedMembers)
                        .externalUserPermission(project.getExternalUserPermission())
                        .textColor(project.getTextColor())
                        .build();
                MossManMod.getProjectRepository().save(updated);
                source.sendMessage(Text.literal("Set " + profile.name() + " as owner of " + prefix).formatted(Formatting.GREEN));
            }
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }

    private static int wipeDatabase(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            source.sendMessage(Text.literal("Wiping MossMan database...").formatted(Formatting.YELLOW));
            MossManMod.getDatabaseManager().dropAndRecreateAllTables();
            source.sendMessage(Text.literal("Database wiped and reinitialized successfully.").formatted(Formatting.GREEN));
            return 1;
        } catch (Exception e) {
            source.sendMessage(TuiHelper.errorText(e));
            return 0;
        }
    }
}
