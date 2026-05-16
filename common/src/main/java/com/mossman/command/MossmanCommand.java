package com.mossman.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mossman.core.model.Project;
import com.mossman.core.usecase.UseCaseException;
import com.mossman.core.usecase.project.CreateProjectUseCase;
import com.mossman.core.usecase.project.ProjectQueries;
import com.mossman.network.ServerProjectSync;
import com.mossman.persistence.JsonProjectRepository;
import com.mossman.server.ServerProjects;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;

/**
 * Smoke-test entry point for the project pipeline: lets a player drive the
 * core use cases from chat instead of the (not-yet-built) UI. Sits next to
 * the C2S handler; both call the same use cases and the same broadcast
 * helper, so observable state stays identical regardless of entry point.
 *
 * <p>This file scaffolds the full Brigadier tree. Leaves marked with
 * {@link #notYetImplemented} are placeholders so tab-completion works
 * end-to-end while follow-up commits fill in the bodies. Update-style
 * leaves take an SNBT {@code <patch>} arg so future patch implementations
 * only need to plug in the merge logic — the wire format is already
 * settled.
 */
public final class MossmanCommand {

    private MossmanCommand() {}

    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, selection) ->
                dispatcher.register(Commands.literal("mossman")
                        .then(createLeaf())
                        .then(listLeaf())
                        .then(deleteLeaf())
                        .then(renameLeaf())
                        .then(ownerLeaf())
                        .then(nonmembersLeaf())
                        .then(showLeaf())
                        .then(roleSubtree())
                        .then(statusSubtree())
                        .then(typeSubtree())
                        .then(ticketSubtree())));
    }

    // ---- project-level leaves -------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> createLeaf() {
        return Commands.literal("create")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> runCreate(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "id"),
                                        StringArgumentType.getString(ctx, "name")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> listLeaf() {
        return Commands.literal("list").executes(ctx -> runList(ctx.getSource()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> deleteLeaf() {
        return Commands.literal("delete")
                .then(Commands.argument("id", StringArgumentType.word())
                        .executes(ctx -> notYetImplemented(ctx.getSource(), "delete")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> renameLeaf() {
        return Commands.literal("rename")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("newName", StringArgumentType.greedyString())
                                .executes(ctx -> notYetImplemented(ctx.getSource(), "rename"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> ownerLeaf() {
        return Commands.literal("owner")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> notYetImplemented(ctx.getSource(), "owner"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> nonmembersLeaf() {
        return Commands.literal("nonmembers")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("allow", BoolArgumentType.bool())
                                .executes(ctx -> notYetImplemented(ctx.getSource(), "nonmembers"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> showLeaf() {
        return Commands.literal("show")
                .then(Commands.argument("id", StringArgumentType.word())
                        .executes(ctx -> notYetImplemented(ctx.getSource(), "show")));
    }

    // ---- role -----------------------------------------------------------
    // SNBT shapes:
    //   create: {name:"Reviewer", color:0xFFAA00, permissions:["VIEW_PROJECT", ...]}
    //   update: any subset of {name, color}  (patch — missing keys keep current)
    //   permissions: {permissions:[...]} — full Set replacement
    private static LiteralArgumentBuilder<CommandSourceStack> roleSubtree() {
        return Commands.literal("role")
                .then(Commands.literal("create")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                        .executes(ctx -> notYetImplemented(ctx.getSource(), "role create")))))
                .then(Commands.literal("update")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("roleId", UuidArgument.uuid())
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> notYetImplemented(ctx.getSource(), "role update"))))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("roleId", UuidArgument.uuid())
                                        .executes(ctx -> notYetImplemented(ctx.getSource(), "role delete")))))
                .then(Commands.literal("permissions")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("roleId", UuidArgument.uuid())
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> notYetImplemented(ctx.getSource(), "role permissions"))))))
                .then(Commands.literal("assign")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("roleId", UuidArgument.uuid())
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> notYetImplemented(ctx.getSource(), "role assign"))))))
                .then(Commands.literal("unassign")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("roleId", UuidArgument.uuid())
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> notYetImplemented(ctx.getSource(), "role unassign"))))))
                .then(Commands.literal("reorder")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("orderedIds", StringArgumentType.greedyString())
                                        .executes(ctx -> notYetImplemented(ctx.getSource(), "role reorder")))));
    }

    // ---- status ---------------------------------------------------------
    // SNBT shapes:
    //   create: {name:"In Review", textColor:0xFFFFFF, backgroundColor:0x6B21A8}
    //   update: any subset of those keys (patch)
    private static LiteralArgumentBuilder<CommandSourceStack> statusSubtree() {
        return Commands.literal("status")
                .then(Commands.literal("create")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                        .executes(ctx -> notYetImplemented(ctx.getSource(), "status create")))))
                .then(Commands.literal("update")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("statusId", UuidArgument.uuid())
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> notYetImplemented(ctx.getSource(), "status update"))))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("statusId", UuidArgument.uuid())
                                        .then(Commands.argument("replacementId", UuidArgument.uuid())
                                                .executes(ctx -> notYetImplemented(ctx.getSource(), "status delete"))))))
                .then(Commands.literal("reorder")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("orderedIds", StringArgumentType.greedyString())
                                        .executes(ctx -> notYetImplemented(ctx.getSource(), "status reorder")))));
    }

    // ---- type (parallel to status) -------------------------------------
    private static LiteralArgumentBuilder<CommandSourceStack> typeSubtree() {
        return Commands.literal("type")
                .then(Commands.literal("create")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                        .executes(ctx -> notYetImplemented(ctx.getSource(), "type create")))))
                .then(Commands.literal("update")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("typeId", UuidArgument.uuid())
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> notYetImplemented(ctx.getSource(), "type update"))))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("typeId", UuidArgument.uuid())
                                        .then(Commands.argument("replacementId", UuidArgument.uuid())
                                                .executes(ctx -> notYetImplemented(ctx.getSource(), "type delete"))))))
                .then(Commands.literal("reorder")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("orderedIds", StringArgumentType.greedyString())
                                        .executes(ctx -> notYetImplemented(ctx.getSource(), "type reorder")))));
    }

    // ---- ticket --------------------------------------------------------
    // SNBT shapes:
    //   create: {title, description?, assignee?, statusId?, typeId?}
    //   update: any subset of {title, description}  (already patch-style in the use case)
    private static LiteralArgumentBuilder<CommandSourceStack> ticketSubtree() {
        return Commands.literal("ticket")
                .then(Commands.literal("create")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                        .executes(ctx -> notYetImplemented(ctx.getSource(), "ticket create")))))
                .then(Commands.literal("update")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("ticketId", UuidArgument.uuid())
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> notYetImplemented(ctx.getSource(), "ticket update"))))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("ticketId", UuidArgument.uuid())
                                        .executes(ctx -> notYetImplemented(ctx.getSource(), "ticket delete")))))
                .then(Commands.literal("assign")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("ticketId", UuidArgument.uuid())
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> notYetImplemented(ctx.getSource(), "ticket assign"))))))
                .then(Commands.literal("status")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("ticketId", UuidArgument.uuid())
                                        .then(Commands.argument("statusId", UuidArgument.uuid())
                                                .executes(ctx -> notYetImplemented(ctx.getSource(), "ticket status"))))))
                .then(Commands.literal("type")
                        .then(Commands.argument("projectId", StringArgumentType.word())
                                .then(Commands.argument("ticketId", UuidArgument.uuid())
                                        .then(Commands.argument("typeId", UuidArgument.uuid())
                                                .executes(ctx -> notYetImplemented(ctx.getSource(), "ticket type"))))));
    }

    // ---- implemented handlers ------------------------------------------

    private static int runCreate(CommandSourceStack source, String id, String name) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return notPlayer(source);
        JsonProjectRepository repo = requireRepo(source);
        if (repo == null) return 0;

        try {
            Project created = new CreateProjectUseCase(repo).execute(actor.getUUID(), id, name);
            source.sendSuccess(() -> Component.literal("Created project " + created.id()), false);
            ServerProjectSync.broadcastProject(
                    source.getServer().getPlayerList().getPlayers(), created);
            return 1;
        } catch (UseCaseException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int runList(CommandSourceStack source) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return notPlayer(source);
        JsonProjectRepository repo = requireRepo(source);
        if (repo == null) return 0;

        List<Project> visible = new ProjectQueries(repo).listProjects(actor.getUUID());
        if (visible.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No projects you can view."), false);
            return 0;
        }
        visible.stream()
                .sorted(Comparator.comparing(Project::id))
                .forEach(p -> source.sendSuccess(
                        () -> Component.literal(p.id() + " — " + p.name()), false));
        return visible.size();
    }

    // ---- shared helpers ------------------------------------------------

    private static int notYetImplemented(CommandSourceStack source, String path) {
        source.sendFailure(Component.literal(path + ": not yet implemented"));
        return 0;
    }

    private static int notPlayer(CommandSourceStack source) {
        source.sendFailure(Component.literal("Only players can use /mossman."));
        return 0;
    }

    private static JsonProjectRepository requireRepo(CommandSourceStack source) {
        JsonProjectRepository repo = ServerProjects.repository().orElse(null);
        if (repo == null) {
            source.sendFailure(Component.literal("Project storage is not ready."));
        }
        return repo;
    }
}
