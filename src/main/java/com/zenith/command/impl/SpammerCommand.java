package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.module.impl.Spammer;

import java.util.ArrayList;
import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class SpammerCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("spammer")
            .category(CommandCategory.MODULE)
            .description("""
            Spams messages or whispers in-game. Use with caution, this can and will get you muted.
            """)
            .usageLines(
                "on/off",
                "whisper on/off",
                "whilePlayerConnected on/off",
                "delayTicks <int>",
                "randomOrder on/off",
                "appendRandom on/off",
                "list",
                "clear",
                "add <message>",
                "addAt <index> <message>",
                "del <index>"
            )
            .aliases("spam")
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("spammer")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.spammer.enabled = getToggle(c, "toggle");
                MODULE.get(Spammer.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("Spammer " + toggleStrCaps(CONFIG.client.extra.spammer.enabled));
                return OK;
            }))
            .then(literal("whisper")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.spammer.whisper = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Whisper " + toggleStrCaps(CONFIG.client.extra.spammer.whisper));
                            return OK;
                      })))
            .then(literal("whilePlayerConnected")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.spammer.whilePlayerConnected = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("While Player Connected " + toggleStrCaps(CONFIG.client.extra.spammer.whilePlayerConnected));
                            return OK;
                      })))
            .then(literal("delayTicks")
                      .then(argument("delayTicks", integer(1)).executes(c -> {
                          CONFIG.client.extra.spammer.delayTicks = IntegerArgumentType.getInteger(c, "delayTicks");
                          c.getSource().getEmbed()
                              .title("Delay Updated!");
                          return OK;
                      })))
            .then(literal("randomOrder")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.spammer.randomOrder = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Random Order " + toggleStrCaps(CONFIG.client.extra.spammer.randomOrder));
                            return OK;
                      })))
            .then(literal("appendRandom")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.spammer.appendRandom = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Append Random " + toggleStrCaps(CONFIG.client.extra.spammer.appendRandom));
                            return OK;
                      })))
            .then(literal("list").executes(c -> {
                c.getSource().getEmbed()
                    .title("Status");
                return OK;
            }))
            .then(literal("clear").executes(c -> {
                CONFIG.client.extra.spammer.messages.clear();
                c.getSource().getEmbed()
                    .title("Messages Cleared!");
                return OK;
            }))
            .then(literal("add")
                      .then(argument("message", greedyString()).executes(c -> {
                          final String message = StringArgumentType.getString(c, "message");
                          CONFIG.client.extra.spammer.messages.add(message);
                          c.getSource().getEmbed()
                                                 .primaryColor()
                                                 .title("Message Added!");
                          return OK;
                      })))
            .then(literal("addAt")
                      .then(argument("index", integer(0))
                                .then(argument("message", greedyString()).executes(c -> {
                                    final int index = IntegerArgumentType.getInteger(c, "index");
                                    final String message = StringArgumentType.getString(c, "message");
                                    try {
                                        CONFIG.client.extra.spammer.messages.add(index, message);
                                        c.getSource().getEmbed()
                                            .title("Message Added!");
                                        return OK;
                                    } catch (final Exception e) {
                                        c.getSource().getEmbed()
                                            .title("Invalid Index!");
                                        return ERROR;
                                    }
                                }))))
            .then(literal("del")
                      .then(argument("index", integer(0)).executes(c -> {
                          final int index = IntegerArgumentType.getInteger(c, "index");
                          try {
                              CONFIG.client.extra.spammer.messages.remove(index);
                              addListDescription(c.getSource().getEmbed()
                                                     .title("Message Removed!"));
                              return OK;
                          } catch (final Exception e) {
                                c.getSource().getEmbed()
                                    .title("Invalid Index!");
                                return ERROR;
                          }
                      })));
    }

    @Override
    public void defaultEmbed(final Embed builder) {
        addListDescription(builder)
            .description("""
                 **WARNING:** This module can and will get you muted on 2b2t or other servers. Use at your own risk.
                 """)
            .addField("Spammer", toggleStr(CONFIG.client.extra.spammer.enabled), false)
            .addField("Whisper", toggleStr(CONFIG.client.extra.spammer.whisper), false)
            .addField("While Player Connected", toggleStr(CONFIG.client.extra.spammer.whilePlayerConnected), false)
            .addField("Delay", CONFIG.client.extra.spammer.delayTicks, false)
            .addField("Random Order", toggleStr(CONFIG.client.extra.spammer.randomOrder), false)
            .addField("Append Random", toggleStr(CONFIG.client.extra.spammer.appendRandom), false)
            .primaryColor();
    }

    private Embed addListDescription(final Embed embedBuilder) {
        final List<String> messages = new ArrayList<>();
        for (int index = 0; index < CONFIG.client.extra.spammer.messages.size(); index++) {
            messages.add("`" + index + ":` " + CONFIG.client.extra.spammer.messages.get(index));
        }
        return embedBuilder.description(String.join("\n", messages));
    }
}
