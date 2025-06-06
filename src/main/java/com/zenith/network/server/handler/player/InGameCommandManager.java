package com.zenith.network.server.handler.player;

import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandOutputHelper;
import com.zenith.command.api.CommandSources;
import com.zenith.network.server.ServerSession;
import com.zenith.util.ComponentSerializer;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.*;

public class InGameCommandManager {

    // this is specific to CONTROLLING account commands - not spectator player commands!
    // true = command was handled
    // false = command was not handled
    public boolean handleInGameCommand(final String message, final @NonNull ServerSession session, final boolean printUnhandled) {
        TERMINAL_LOG.info("{} executed in-game command: {}", session.getName(), message);
        final String command = message.split(" ")[0]; // first word is the command
        if (command.equals("help") && CONFIG.inGameCommands.enable && !CONFIG.inGameCommands.slashCommands) {
            session.sendAsync(new ClientboundSystemChatPacket(
                ComponentSerializer.minimessage("<blue><bold>In Game commands"),
                false));
            session.sendAsync(new ClientboundSystemChatPacket(
                ComponentSerializer.minimessage("<green>Prefix : \"" + CONFIG.inGameCommands.prefix + "\""),
                false));
        }
        return executeInGameCommand(message, session, printUnhandled);
    }

    public boolean isCommandPrefixed(final String message) {
        return message.startsWith(CONFIG.inGameCommands.prefix);
    }

    private boolean executeInGameCommand(final String command, final ServerSession session, final boolean printUnhandled) {
        final CommandContext commandContext = CommandContext.createInGamePlayerContext(command, session);
        var parse = COMMAND.parse(commandContext);
        if (!COMMAND.hasCommandNode(parse)) return false;
        EXECUTOR.execute(() -> {
            COMMAND.execute(commandContext, parse);
            var embed = commandContext.getEmbed();
            CommandOutputHelper.logEmbedOutputToInGame(embed, session);
            CommandOutputHelper.logMultiLineOutputToInGame(commandContext.getMultiLineOutput(), session);
            if (!commandContext.isNoOutput() && !embed.isTitlePresent() && commandContext.getMultiLineOutput().isEmpty()) {
                if (printUnhandled) {
                    session.sendAsyncAlert("<red>Unknown command!");
                }
                return;
            }
            if (CONFIG.inGameCommands.logToDiscord && DISCORD.isRunning() && !commandContext.isSensitiveInput()) {
                // will also log to terminal
                CommandOutputHelper.logInputToDiscord(command, CommandSources.PLAYER, commandContext);
                CommandOutputHelper.logEmbedOutputToDiscord(embed);
                CommandOutputHelper.logMultiLineOutputToDiscord(commandContext.getMultiLineOutput());
            } else {
                CommandOutputHelper.logEmbedOutputToTerminal(embed);
                CommandOutputHelper.logMultiLineOutputToTerminal(commandContext.getMultiLineOutput());
            }
        });
        return true;
    }

    public void handleInGameCommandSpectator(final String message, final @NonNull ServerSession session, final boolean printUnhandled) {
        TERMINAL_LOG.info("{} executed in-game spectator command: {}", session.getName(), message);
        final CommandContext commandContext = CommandContext.createSpectatorContext(message, session);
        var parse = COMMAND.parse(commandContext);
        if (COMMAND.hasCommandNode(parse)) {
            COMMAND.execute(commandContext, parse);
        }
        var embed = commandContext.getEmbed();
        CommandOutputHelper.logEmbedOutputToInGame(embed, session);
        CommandOutputHelper.logMultiLineOutputToInGame(commandContext.getMultiLineOutput(), session);
        if (!commandContext.isNoOutput() && !embed.isTitlePresent() && commandContext.getMultiLineOutput().isEmpty()) {
            if (printUnhandled) {
                session.sendAsyncAlert("<red>Unknown command!");
            }
            return;
        }
        if (CONFIG.inGameCommands.logToDiscord && DISCORD.isRunning() && !commandContext.isSensitiveInput()) {
            // will also log to terminal
            CommandOutputHelper.logInputToDiscord(message, CommandSources.SPECTATOR, commandContext);
            CommandOutputHelper.logEmbedOutputToDiscord(embed);
            CommandOutputHelper.logMultiLineOutputToDiscord(commandContext.getMultiLineOutput());
        } else {
            CommandOutputHelper.logEmbedOutputToTerminal(embed);
            CommandOutputHelper.logMultiLineOutputToTerminal(commandContext.getMultiLineOutput());
        }
    }
}
