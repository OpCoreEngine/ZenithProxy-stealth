package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.cache.data.chunk.ChunkCache;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveMobEffectPacket;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.CONFIG;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static com.zenith.util.DisconnectMessages.MANUAL_DISCONNECT;
import static java.util.Arrays.asList;

public class DebugCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("debug")
            .category(CommandCategory.MANAGE)
            .description("""
            Debug settings for features in testing or for use in development.
            """)
            .usageLines(
                "sync inventory",
                "sync chunks",
                "clearEffects",
                "packetLog on/off",
                "packetLog client on/off", // todo: subcommands for configuring subsettings more explicitly
                "packetLog server on/off",
                "packetLog filter <string>",
                "kickDisconnect on/off",
                "dc",
                "debugLogs on/off",
                "chunkCacheFullbright on/off"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("debug").requires(Command::validateAccountOwner)
            .then(literal("packetLog")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.debug.packetLog.enabled = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Packet Log " + toggleStrCaps(CONFIG.debug.packetLog.enabled));
                          return OK;
                      }))
                      .then(literal("client")
                                .then(argument("toggle", toggle()).executes(c -> {
                                    var toggle = getToggle(c, "toggle");
                                    if (toggle) {
                                        CONFIG.debug.packetLog.clientPacketLog.received = true;
                                        CONFIG.debug.packetLog.clientPacketLog.receivedBody = true;
                                        CONFIG.debug.packetLog.clientPacketLog.postSent = true;
                                        CONFIG.debug.packetLog.clientPacketLog.postSentBody = true;
                                    } else {
                                        CONFIG.debug.packetLog.clientPacketLog.received = false;
                                        CONFIG.debug.packetLog.clientPacketLog.postSent = false;
                                        CONFIG.debug.packetLog.clientPacketLog.preSent = false;
                                    }
                                    c.getSource().getEmbed()
                                        .title("Client Packet Log " + toggleStrCaps(toggle));
                                    return OK;
                                })))
                      .then(literal("server")
                                .then(argument("toggle", toggle()).executes(c -> {
                                    var toggle = getToggle(c, "toggle");
                                    if (toggle) {
                                        CONFIG.debug.packetLog.serverPacketLog.received = true;
                                        CONFIG.debug.packetLog.serverPacketLog.receivedBody = true;
                                        CONFIG.debug.packetLog.serverPacketLog.postSent = true;
                                        CONFIG.debug.packetLog.serverPacketLog.postSentBody = true;
                                    } else {
                                        CONFIG.debug.packetLog.serverPacketLog.received = false;
                                        CONFIG.debug.packetLog.serverPacketLog.postSent = false;
                                        CONFIG.debug.packetLog.serverPacketLog.preSent = false;
                                    }
                                    c.getSource().getEmbed()
                                        .title("Server Packet Log " + toggleStrCaps(toggle));
                                    return OK;
                                })))
                      .then(literal("filter")
                                .then(argument("filter", wordWithChars()).executes(c -> {
                                    CONFIG.debug.packetLog.packetFilter = c.getArgument("filter", String.class);
                                    if ("off".equalsIgnoreCase(CONFIG.debug.packetLog.packetFilter))
                                        CONFIG.debug.packetLog.packetFilter = "";
                                    c.getSource().getEmbed()
                                        .title("Packet Log Filter Set: " + CONFIG.debug.packetLog.packetFilter);
                                    return OK;
                                })))
                      .then(literal("logLevelDebug").then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.debug.packetLog.logLevelDebug = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Log Level Debug " + toggleStrCaps(CONFIG.debug.packetLog.logLevelDebug));
                          return OK;
                      }))))
            .then(literal("sync")
                        .then(literal("inventory").executes(c -> {
                            PlayerCache.sync();
                            c.getSource().getEmbed()
                                .title("Inventory Synced");
                            return OK;
                        }))
                        .then(literal("chunks").executes(c -> {
                            ChunkCache.sync();
                            c.getSource().getEmbed()
                                .title("Synced Chunks");
                            return OK;
                        })))
            .then(literal("clearEffects").executes(c -> {
                CACHE.getPlayerCache().getThePlayer().getPotionEffectMap().clear();
                var session = Proxy.getInstance().getCurrentPlayer().get();
                if (session != null) {
                    asList(Effect.values()).forEach(effect -> session.sendAsync(new ClientboundRemoveMobEffectPacket(
                        CACHE.getPlayerCache().getEntityId(),
                        effect)));
                }
                c.getSource().getEmbed()
                    .title("Cleared Effects");
                return OK;
            }))
            .then(literal("kickDisconnect").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.debug.kickDisconnect = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Kick Disconnect " + toggleStrCaps(CONFIG.debug.kickDisconnect));
                return OK;
            })))
            // insta disconnect
            .then(literal("dc").executes(c -> {
                c.getSource().setNoOutput(true);
                Proxy.getInstance().kickDisconnect(MANUAL_DISCONNECT, null);
            }))
            .then(literal("debugLogs").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.debug.debugLogs = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Debug Logs " + toggleStrCaps(CONFIG.debug.debugLogs));
                return OK;
            })))
            .then(literal("chunkCacheFullbright").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.debug.server.cache.fullbrightChunkSkylight = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Chunk Cache Fullbright " + toggleStrCaps(CONFIG.debug.server.cache.fullbrightChunkSkylight));
                return OK;
            })))
            .then(literal("binaryNbtComponentSerializer").then(argument("toggle", toggle()).executes(c -> {
                MinecraftTypes.useBinaryNbtComponentSerializer = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Binary NBT Component Serializer " + toggleStrCaps(MinecraftTypes.useBinaryNbtComponentSerializer));
                return OK;
            })));
    }

    @Override
    public void defaultEmbed(final Embed builder) {
        builder
            .addField("Packet Log", toggleStr(CONFIG.debug.packetLog.enabled), false)
            .addField("Client Packet Log", toggleStr(CONFIG.debug.packetLog.clientPacketLog.received), false)
            .addField("Server Packet Log", toggleStr(CONFIG.debug.packetLog.serverPacketLog.received), false)
            .addField("Packet Log Filter", CONFIG.debug.packetLog.packetFilter, false)
            .addField("Kick Disconnect", toggleStr(CONFIG.debug.kickDisconnect), false)
            .addField("Debug Logs", toggleStr(CONFIG.debug.debugLogs), false)
            .addField("Chunk Cache Fullbright", toggleStr(CONFIG.debug.server.cache.fullbrightChunkSkylight), false)
            .primaryColor();
    }
}
