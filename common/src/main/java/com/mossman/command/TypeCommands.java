package com.mossman.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mossman.command.Mutations.Result;
import com.mossman.core.model.Project;
import com.mossman.core.model.TicketType;
import com.mossman.core.usecase.NotFoundException;
import com.mossman.core.usecase.UseCaseException;
import com.mossman.core.usecase.project.ProjectQueries;
import com.mossman.core.usecase.type.CreateTypeUseCase;
import com.mossman.core.usecase.type.DeleteTypeUseCase;
import com.mossman.core.usecase.type.ReorderTypesUseCase;
import com.mossman.core.usecase.type.UpdateTypeUseCase;
import com.mossman.persistence.JsonProjectRepository;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** {@code /mossman project config type ...} — parallel to {@link StatusCommands}. */
final class TypeCommands {

    private TypeCommands() {}

    static LiteralArgumentBuilder<CommandSourceStack> subtree() {
        return Commands.literal("type")
                .then(Commands.literal("list")
                        .then(CommandArgs.projectIdArg()
                                .executes(ctx -> list(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "projectId")))))
                .then(Commands.literal("view")
                        .then(CommandArgs.projectIdArg()
                                .then(CommandArgs.typeIdArg()
                                        .executes(ctx -> view(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "typeId"))))))
                .then(Commands.literal("create")
                        .then(CommandArgs.projectIdArg()
                                .then(Commands.argument("typeId", StringArgumentType.string())
                                        .executes(ctx -> create(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "typeId"),
                                                null))
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> create(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "typeId"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("delete")
                        .then(CommandArgs.projectIdArg()
                                .then(CommandArgs.typeIdArg()
                                        .executes(ctx -> delete(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "projectId"),
                                                StringArgumentType.getString(ctx, "typeId"),
                                                null))
                                        .then(Commands.argument("replacementTypeId", StringArgumentType.string())
                                                .suggests(Suggestions.typeNames("projectId"))
                                                .executes(ctx -> delete(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "typeId"),
                                                        StringArgumentType.getString(ctx, "replacementTypeId")))))))
                .then(Commands.literal("update")
                        .then(CommandArgs.projectIdArg()
                                .then(CommandArgs.typeIdArg()
                                        .then(Commands.argument("patch", CompoundTagArgument.compoundTag())
                                                .executes(ctx -> update(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "typeId"),
                                                        CompoundTagArgument.getCompoundTag(ctx, "patch")))))))
                .then(Commands.literal("order")
                        .then(CommandArgs.projectIdArg()
                                .then(CommandArgs.typeIdArg()
                                        .then(Commands.argument("position", IntegerArgumentType.integer(1))
                                                .executes(ctx -> order(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "projectId"),
                                                        StringArgumentType.getString(ctx, "typeId"),
                                                        IntegerArgumentType.getInteger(ctx, "position")))))));
    }

    private static int list(CommandSourceStack source, String projectId) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return Mutations.notPlayer(source);
        JsonProjectRepository repo = Mutations.requireRepo(source);
        if (repo == null) return 0;
        try {
            Project p = new ProjectQueries(repo).getProject(actor.getUUID(), projectId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
            for (TicketType t : p.types()) {
                Mutations.reportComponent(source, Component.empty()
                        .append(ChatHelpers.applyColor(Component.literal(t.name()), t.textColor()))
                        .append(" ")
                        .append(ChatHelpers.createRunLink("[view]",
                                "/mossman project config type view " + projectId + " " + Mutations.q(t.name()),
                                "View type " + t.name(), ChatFormatting.GREEN)));
            }
            Mutations.reportComponent(source, Mutations.buttonRow(
                    ChatHelpers.createSuggestLink("[create]",
                            "/mossman project config type create " + projectId + " ",
                            "Create a new type", ChatFormatting.YELLOW)));
            return p.types().size();
        } catch (UseCaseException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int view(CommandSourceStack source, String projectId, String typeName) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return Mutations.notPlayer(source);
        JsonProjectRepository repo = Mutations.requireRepo(source);
        if (repo == null) return 0;
        try {
            Project p = new ProjectQueries(repo).getProject(actor.getUUID(), projectId)
                    .orElseThrow(() -> new NotFoundException(NotFoundException.Kind.PROJECT, projectId));
            TicketType t = Resolvers.resolveType(p, typeName);
            Mutations.reportComponent(source, Component.literal("== Type: ")
                    .append(ChatHelpers.applyColor(Component.literal(t.name()), t.textColor()))
                    .append(" =="));
            Mutations.reportComponent(source, Component.literal("Text color: ")
                    .append(ChatHelpers.colorSwatch(t.textColor())));
            Mutations.reportComponent(source, Component.literal("Background:  ")
                    .append(ChatHelpers.colorSwatch(t.backgroundColor())));
            long tickets = p.tickets().stream().filter(tk -> tk.typeId().equals(t.id())).count();
            Mutations.report(source, "Tickets of this type: " + tickets);
            String qName = Mutations.q(t.name());
            Mutations.reportComponent(source, Mutations.buttonRow(
                    ChatHelpers.createSuggestLink("[update]",
                            "/mossman project config type update " + projectId + " " + qName + " {}",
                            "Update type fields", ChatFormatting.YELLOW),
                    ChatHelpers.createSuggestLink("[order]",
                            "/mossman project config type order " + projectId + " " + qName + " ",
                            "Reorder this type", ChatFormatting.YELLOW),
                    ChatHelpers.createSuggestLink("[delete]",
                            "/mossman project config type delete " + projectId + " " + qName,
                            "Delete this type", ChatFormatting.RED)));
            return 1;
        } catch (UseCaseException | IllegalArgumentException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int create(CommandSourceStack source, String projectId, String typeName, CompoundTag patch) {
        return Mutations.mutate(source, ctx -> {
            SnbtPatch p = patch == null ? new SnbtPatch(new CompoundTag()) : new SnbtPatch(patch);
            int textColor = p.optionalInt("textColor").orElse(0xFFFFFF);
            int bgColor = p.optionalInt("backgroundColor").orElse(0x000000);
            new CreateTypeUseCase(ctx.repo()).execute(ctx.actor().getUUID(), projectId, typeName, textColor, bgColor);
            Project updated = ctx.repo().find(projectId).orElseThrow();
            return Result.broadcast(updated, "Created type " + typeName);
        });
    }

    private static int delete(CommandSourceStack source, String projectId, String typeName,
                              String replacementName) {
        return Mutations.mutate(source, ctx -> {
            Project current = Resolvers.requireProject(ctx.repo(), projectId);
            TicketType toDelete = Resolvers.resolveType(current, typeName);
            TicketType replacement = replacementName == null
                    ? current.types().stream()
                            .filter(t -> !t.id().equals(toDelete.id()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "cannot delete the only type in '" + projectId + "'"))
                    : Resolvers.resolveType(current, replacementName);
            new DeleteTypeUseCase(ctx.repo())
                    .execute(ctx.actor().getUUID(), projectId, toDelete.id(), replacement.id());
            Project updated = ctx.repo().find(projectId).orElseThrow();
            return Result.broadcast(updated,
                    "Deleted type " + typeName + " (tickets moved to " + replacement.name() + ")");
        });
    }

    private static int update(CommandSourceStack source, String projectId, String typeName, CompoundTag patch) {
        return Mutations.mutate(source, ctx -> {
            Project current = Resolvers.requireProject(ctx.repo(), projectId);
            TicketType existing = Resolvers.resolveType(current, typeName);
            SnbtPatch p = new SnbtPatch(patch);
            String name = p.optionalString("name").orElse(existing.name());
            int textColor = p.optionalInt("textColor").orElse(existing.textColor());
            int bgColor = p.optionalInt("backgroundColor").orElse(existing.backgroundColor());
            new UpdateTypeUseCase(ctx.repo())
                    .execute(ctx.actor().getUUID(), projectId, existing.id(), name, textColor, bgColor);
            Project updated = ctx.repo().find(projectId).orElseThrow();
            return Result.broadcast(updated, "Updated type " + typeName);
        });
    }

    private static int order(CommandSourceStack source, String projectId, String typeName, int position) {
        return Mutations.mutate(source, ctx -> {
            Project current = Resolvers.requireProject(ctx.repo(), projectId);
            UUID id = Resolvers.resolveType(current, typeName).id();
            List<UUID> order = Resolvers.moveToPosition(
                    current.types().stream().map(TicketType::id).collect(Collectors.toList()),
                    id, position);
            new ReorderTypesUseCase(ctx.repo()).execute(ctx.actor().getUUID(), projectId, order);
            Project updated = ctx.repo().find(projectId).orElseThrow();
            return Result.broadcast(updated, "Moved type " + typeName + " to position " + position);
        });
    }
}
