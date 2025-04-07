package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.module.impl.AntiAFK;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class AntiAFKCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("antiAFK")
            .category(CommandCategory.MODULE)
            .description("""
            Configures the AntiAFK module.
            
            To avoid being kicked on 2b2t the only required action is swing OR walk.
            
            The walk action will move the player roughly in a square shape. To avoid falling down any ledges, enable safeWalk
            
            For delay settings, 1 tick = 50ms
            """)
            .usageLines(
                "on/off",
                "rotate on/off",
                "rotate delay <ticks>",
                "swing on/off",
                "swing delay <ticks>",
                "walk on/off",
                "walk delay <ticks>",
                "safeWalk on/off",
                "walkDistance <ticks>",
                "jump on/off",
                "jump onlyInWater on/off",
                "jump delay <int>",
                "sneak on/off"
            )
            .aliases(
                "afk"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("antiAFK")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.antiafk.enabled = getToggle(c, "toggle");
                MODULE.get(AntiAFK.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("AntiAFK " + toggleStrCaps(CONFIG.client.extra.antiafk.enabled));
                return OK;
            }))
            .then(literal("rotate")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.client.extra.antiafk.actions.rotate = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Rotate " + toggleStrCaps(CONFIG.client.extra.antiafk.actions.rotate));
                          return OK;
                      }))
                      .then(literal("delay").then(argument("delay", integer(1, 50000)).executes(c -> {
                          CONFIG.client.extra.antiafk.actions.rotateDelayTicks = IntegerArgumentType.getInteger(c, "delay");
                          c.getSource().getEmbed()
                              .title("Rotate Delay Set!");
                          return OK;
                      }))))
            .then(literal("swing")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.client.extra.antiafk.actions.swingHand = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Swing " + toggleStrCaps(CONFIG.client.extra.antiafk.actions.swingHand));
                          return OK;
                      }))
                      .then(literal("delay").then(argument("delay", integer(1, 50000)).executes(c -> {
                          CONFIG.client.extra.antiafk.actions.swingDelayTicks = IntegerArgumentType.getInteger(c, "delay");
                          c.getSource().getEmbed()
                              .title("Swing Delay Set!");
                          return OK;
                      }))))
            .then(literal("walk")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.client.extra.antiafk.actions.walk = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Walk " + toggleStrCaps(CONFIG.client.extra.antiafk.actions.walk));
                          return OK;
                      }))
                      .then(literal("delay").then(argument("delay", integer(1, 50000)).executes(c -> {
                          CONFIG.client.extra.antiafk.actions.walkDelayTicks = IntegerArgumentType.getInteger(c, "delay");
                          c.getSource().getEmbed()
                              .title("Walk Delay Set!");
                          return OK;
                      }))))
            .then(literal("safeWalk")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.client.extra.antiafk.actions.safeWalk = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("SafeWalk " + toggleStrCaps(CONFIG.client.extra.antiafk.actions.safeWalk));
                          return OK;
                      })))
            .then(literal("walkDistance")
                                .then(argument("walkdist", integer(1)).executes(c -> {
                                    CONFIG.client.extra.antiafk.actions.walkDistance = IntegerArgumentType.getInteger(c, "walkdist");
                                    c.getSource().getEmbed()
                                        .title("Walk Distance Set!");
                                    return OK;
                                })))
            .then(literal("jump")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.client.extra.antiafk.actions.jump = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Jump " + toggleStrCaps(CONFIG.client.extra.antiafk.actions.jump));
                          return OK;
                      }))
                      .then(literal("onlyInWater").then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.antiafk.actions.jumpOnlyInWater = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Jump Only In Water " + toggleStrCaps(CONFIG.client.extra.antiafk.actions.jumpOnlyInWater));
                            return OK;
                      })))
                      .then(literal("delay").then(argument("delay", integer(1, 50000)).executes(c -> {
                          CONFIG.client.extra.antiafk.actions.jumpDelayTicks = IntegerArgumentType.getInteger(c, "delay");
                          c.getSource().getEmbed()
                              .title("Jump Delay Set!");
                          return OK;
                      }))))
            .then(literal("sneak")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.client.extra.antiafk.actions.sneak = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Sneak " + toggleStrCaps(CONFIG.client.extra.antiafk.actions.sneak));
                          return OK;
                      })));
    }

    @Override
    public void postPopulate(final Embed embedBuilder) {
        embedBuilder
            .addField("AntiAFK", toggleStr(CONFIG.client.extra.antiafk.enabled), false)
            .addField("Rotate", toggleStr(CONFIG.client.extra.antiafk.actions.rotate)
                + " - Delay: " + CONFIG.client.extra.antiafk.actions.rotateDelayTicks, false)
            .addField("Swing", toggleStr(CONFIG.client.extra.antiafk.actions.swingHand)
                + " - Delay: " + CONFIG.client.extra.antiafk.actions.swingDelayTicks, false)
            .addField("Walk", toggleStr(CONFIG.client.extra.antiafk.actions.walk)
                + " - Delay: " + CONFIG.client.extra.antiafk.actions.walkDelayTicks, false)
            .addField("Safe Walk", toggleStr(CONFIG.client.extra.antiafk.actions.safeWalk), false)
            .addField("Walk Distance", CONFIG.client.extra.antiafk.actions.walkDistance, false)
            .addField("Jump", toggleStr(CONFIG.client.extra.antiafk.actions.jump)
                + " - Only In Water: " + toggleStr(CONFIG.client.extra.antiafk.actions.jumpOnlyInWater)
                + " - Delay: " + CONFIG.client.extra.antiafk.actions.jumpDelayTicks, false)
            .addField("Sneak", toggleStr(CONFIG.client.extra.antiafk.actions.sneak), false)
            .primaryColor();
    }
}
