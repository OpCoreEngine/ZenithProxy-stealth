package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.command.brigadier.ToggleArgumentType;
import com.zenith.feature.motdspoofer.MotdSpooferApi;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.saveConfigAsync;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class MotdSpooferCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("motdspoofer")
            .category(CommandCategory.MODULE)
            .description("Configure MOTD spoofing to display another server's MOTD")
            .usageLines(
                "",
                "[on/off]",
                "server <ip> [port]",
                "reload",
                "status"
            )
            .aliases("motd", "spoofmotd")
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return literal("motdspoofer")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.server.motdSpoofer.enabled = getToggle(c, "toggle");
                saveConfigAsync();
                if (CONFIG.server.motdSpoofer.enabled) {
                    MotdSpooferApi.clearCache();
                    c.getSource().getEmbed()
                        .title("MOTD Spoofer Enabled")
                        .primaryColor()
                        .description("Spoofing MOTD from " + CONFIG.server.motdSpoofer.serverIp + ":" + CONFIG.server.motdSpoofer.serverPort);
                } else {
                    c.getSource().getEmbed()
                        .title("MOTD Spoofer Disabled")
                        .primaryColor();
                }
                return 1;
            }))
            .then(literal("server")
                .then(argument("ip", string())
                    .executes(c -> {
                        String ip = StringArgumentType.getString(c, "ip");
                        CONFIG.server.motdSpoofer.serverIp = ip;
                        CONFIG.server.motdSpoofer.serverPort = 25565;
                        MotdSpooferApi.clearCache();
                        saveConfigAsync();
                        c.getSource().getEmbed()
                            .title("MOTD Spoofer Server Updated")
                            .primaryColor()
                            .description("Server set to: " + ip + ":25565");
                        return 1;
                    })
                    .then(argument("port", integer(1, 65535))
                        .executes(c -> {
                            String ip = StringArgumentType.getString(c, "ip");
                            int port = IntegerArgumentType.getInteger(c, "port");
                            CONFIG.server.motdSpoofer.serverIp = ip;
                            CONFIG.server.motdSpoofer.serverPort = port;
                            MotdSpooferApi.clearCache();
                            saveConfigAsync();
                            c.getSource().getEmbed()
                                .title("MOTD Spoofer Server Updated")
                                .primaryColor()
                                .description("Server set to: " + ip + ":" + port);
                            return 1;
                        }))))
            .then(literal("reload").executes(c -> {
                MotdSpooferApi.clearCache();
                c.getSource().getEmbed()
                    .title("MOTD Cache Cleared")
                    .primaryColor()
                    .description("MOTD will be refreshed on next server list ping");
                return 1;
            }))
            .then(literal("status").executes(c -> {
                c.getSource().getEmbed()
                    .title("MOTD Spoofer Status")
                    .primaryColor()
                    .addField("Enabled", CONFIG.server.motdSpoofer.enabled ? "Yes" : "No", false)
                    .addField("Target Server", CONFIG.server.motdSpoofer.serverIp + ":" + CONFIG.server.motdSpoofer.serverPort, false);
                return 1;
            }));
    }
}