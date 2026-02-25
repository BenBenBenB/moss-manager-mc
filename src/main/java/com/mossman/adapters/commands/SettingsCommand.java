package com.mossman.adapters.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mossman.adapters.tui.SuggestionHelper;
import com.mossman.adapters.tui.TuiHelper;
import com.mossman.domain.entities.PlayerSettings;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class SettingsCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralCommandNode<ServerCommandSource> rootNode) {
        var settingsNode = CommandManager.literal("settings")
                .executes(SettingsCommand::viewSettings)
                .then(CommandManager.literal("timezone")
                        .then(CommandManager.argument("zone", StringArgumentType.greedyString())
                                .suggests(SuggestionHelper::suggestCommonTimezones)
                                .executes(SettingsCommand::setTimezone)))
                .build();

        rootNode.addChild(settingsNode);
    }

    private static int viewSettings(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (source.getPlayer() == null) {
            source.sendMessage(Text.literal("Only players can use the settings command.").formatted(Formatting.RED));
            return 0;
        }
        UUID playerId = source.getPlayer().getUuid();
        var settings = com.mossman.MossManMod.getPlayerSettingsRepository().findByPlayerId(playerId);
        String timezone = settings.map(PlayerSettings::timezone).orElse("UTC");
        ZoneId zone = TuiHelper.resolveZone(source);

        source.sendMessage(Text.literal("--- MossMan Settings ---").formatted(Formatting.AQUA));

        String currentTime = DateTimeFormatter.ofPattern("MM/dd HH:mm")
                .format(ZonedDateTime.now(zone));
        source.sendMessage(Text.empty()
                .append(TuiHelper.createSuggestLink("[✎] ", "/mossman settings timezone " + timezone,
                        "Change your timezone", Formatting.GRAY))
                .append(Text.literal("Timezone: " + timezone + " (now: " + currentTime + ")").formatted(Formatting.WHITE)));

        return 1;
    }

    private static int setTimezone(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (source.getPlayer() == null) {
            source.sendMessage(Text.literal("Only players can use the settings command.").formatted(Formatting.RED));
            return 0;
        }
        UUID playerId = source.getPlayer().getUuid();
        String zoneArg = StringArgumentType.getString(context, "zone");

        ZoneId zone;
        try {
            zone = ZoneId.of(zoneArg);
        } catch (Exception e) {
            source.sendMessage(Text.literal("Unknown timezone: \"" + zoneArg
                    + "\". Use an IANA zone ID such as America/New_York, Europe/London, or UTC.").formatted(Formatting.RED));
            return 0;
        }

        com.mossman.MossManMod.getPlayerSettingsRepository().save(new PlayerSettings(playerId, zone.getId()));

        String currentTime = DateTimeFormatter.ofPattern("MM/dd HH:mm")
                .format(ZonedDateTime.now(zone));
        source.sendMessage(Text.literal("Timezone set to " + zone.getId() + " (now: " + currentTime + ").").formatted(Formatting.GREEN));
        return 1;
    }
}
